package com.hyzalia.paint.prefab;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.common.util.PathUtil;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.config.SelectionPrefabSerializer;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.LocalCachedChunkAccessor;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.BsonUtil;
import com.hyzalia.paint.PaintingConstants;
import com.hypixel.hytale.logger.HytaleLogger;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.nio.file.Path;

/**
 * Sauvegarde prefab identique au flux vanilla du menu BuilderTools, puis ré-injecte les fluides
 * depuis le monde pour les cellules de la sélection (corrige la branche {@code Editor_Anchor} sans fluides).
 */
public final class FluidInclusivePrefabSave {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private FluidInclusivePrefabSave() {
    }

    /**
     * Équivalent au lambda vanilla après clic Save (presse-papiers ou sélection monde).
     */
    public static void handle(
            boolean fromClipboard,
            @Nonnull String prefabName,
            boolean overwrite,
            boolean entities,
            boolean empty,
            boolean clearSupport,
            Vector3i playerAnchorWorldOrNull,
            AssetPack pack,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull BuilderToolsPlugin.BuilderState builderState,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (fromClipboard) {
            builderState.save(
                    ref,
                    prefabName,
                    true,
                    overwrite,
                    clearSupport,
                    pack,
                    accessor);
            return;
        }

        builderState.saveFromSelection(
                ref,
                prefabName,
                true,
                overwrite,
                entities,
                empty,
                playerAnchorWorldOrNull,
                clearSupport,
                pack,
                accessor);

        String resolvedKey = resolvePrefabFileKey(prefabName);
        try {
            enrichFluidsFromWorld(builderState, accessor, pack, resolvedKey);
        } catch (RuntimeException ex) {
            LOGGER.atWarning().withCause(ex).log(
                    "[" + PaintingConstants.SERVER_NAME + "] Échec enrichissement fluides après prefab save ("
                            + resolvedKey + ")");
        }
    }

    static Vector3i computePlayerAnchorWorld(
            Ref<EntityStore> ref,
            Store<EntityStore> store,
            boolean usePlayerAnchor,
            boolean fromClipboard) {
        if (!usePlayerAnchor || fromClipboard) {
            return null;
        }
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return null;
        }
        var pos = transform.getPosition();
        return new Vector3i(
                MathUtil.floor(pos.x()),
                MathUtil.floor(pos.y()),
                MathUtil.floor(pos.z()));
    }

    private static String resolvePrefabFileKey(String name) {
        if (name.endsWith(".prefab.json")) {
            return name;
        }
        return name + ".prefab.json";
    }

    private static void enrichFluidsFromWorld(
            BuilderToolsPlugin.BuilderState builderState,
            ComponentAccessor<EntityStore> accessor,
            AssetPack pack,
            String prefabKey) {
        World world = accessor.getExternalData().getWorld();
        BlockSelection marquee = builderState.getSelection();
        if (marquee == null || !marquee.hasSelectionBounds()) {
            return;
        }

        Vector3i min = Vector3iUtil.min(marquee.getSelectionMin(), marquee.getSelectionMax());
        Vector3i max = Vector3iUtil.max(marquee.getSelectionMin(), marquee.getSelectionMax());

        PrefabStore store = PrefabStore.get();
        Path root = store.getAssetPrefabsPathForPack(pack);
        Path path = PathUtil.resolvePathWithinDir(root, prefabKey);
        if (path == null) {
            LOGGER.atWarning().log(
                    "[" + PaintingConstants.SERVER_NAME + "] Chemin prefab invalide pour enrichissement fluides: "
                            + prefabKey);
            return;
        }

        BsonDocument doc = BsonUtil.readDocumentNow(path);
        BlockSelection saved = SelectionPrefabSerializer.deserialize(doc);

        /*
         * Les bounds désérialisés (min/max) peuvent être en coordonnées monde dans certains prefabs, alors que les
         * entrées "blocks" sont en local — d’où des fluides encore en "world" si on s’appuie sur selectionMin.
         * Le repère correct est le coin mini des blocs en espace local (comme en vanilla à l’écriture).
         */
        LocalBlockBounds blockBounds = computeLocalBlockBounds(saved);

        clearSelectionFluids(saved);

        int minX = min.x();
        int maxX = max.x();
        int minZ = min.z();
        int maxZ = max.z();
        int dx = maxX - minX;
        int dz = maxZ - minZ;
        int radius = Math.max(dx, dz);
        LocalCachedChunkAccessor accessorChunks = LocalCachedChunkAccessor.atWorldCoords(
                world,
                minX + dx / 2,
                minZ + dz / 2,
                radius);

        int minY = min.y();
        int maxY = max.y();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                WorldChunk chunk = accessorChunks.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
                if (chunk == null) {
                    continue;
                }
                for (int y = minY; y <= maxY; y++) {
                    FluidSample fluid = readFluidSample(chunk, x, y, z);
                    if (fluid.fluidId != 0 || fluid.level != 0) {
                        if (blockBounds.any()) {
                            int lx = blockBounds.minX() + (x - minX);
                            int ly = blockBounds.minY() + (y - minY);
                            int lz = blockBounds.minZ() + (z - minZ);
                            saved.addFluidAtLocalPos(lx, ly, lz, fluid.fluidId, fluid.level);
                        } else {
                            saved.addFluidAtWorldPos(x, y, z, fluid.fluidId, fluid.level);
                        }
                    }
                }
            }
        }

        if (pack != null) {
            store.savePrefabToPack(pack, prefabKey, saved, true);
        } else {
            store.saveServerPrefab(prefabKey, saved, true);
        }
    }

    private record LocalBlockBounds(int minX, int minY, int minZ, boolean any) {
        static LocalBlockBounds empty() {
            return new LocalBlockBounds(0, 0, 0, false);
        }
    }

    private static LocalBlockBounds computeLocalBlockBounds(BlockSelection saved) {
        int[] min = new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        boolean[] any = {false};
        saved.forEachBlock((bx, by, bz, holder) -> {
            any[0] = true;
            min[0] = Math.min(min[0], bx);
            min[1] = Math.min(min[1], by);
            min[2] = Math.min(min[2], bz);
        });
        if (!any[0]) {
            return LocalBlockBounds.empty();
        }
        return new LocalBlockBounds(min[0], min[1], min[2], true);
    }

    /**
     * Vide la carte interne des fluides pour éviter de garder d’anciennes entrées (ex. coords monde erronées)
     * avant de réinjecter depuis le monde.
     */
    private static void clearSelectionFluids(BlockSelection saved) {
        try {
            Field field = BlockSelection.class.getDeclaredField("fluids");
            field.setAccessible(true);
            Object map = field.get(saved);
            map.getClass().getMethod("clear").invoke(map);
        } catch (ReflectiveOperationException ex) {
            LOGGER.atWarning().withCause(ex).log(
                    "[" + PaintingConstants.SERVER_NAME + "] Impossible de vider les fluides de la sélection, "
                            + "réécriture sans clear");
        }
    }

    private record FluidSample(int fluidId, byte level) {
    }

    @SuppressWarnings("deprecation")
    private static FluidSample readFluidSample(WorldChunk worldChunk, int x, int y, int z) {
        var chunkRef = worldChunk.getReference();
        var store = chunkRef.getStore();
        ChunkColumn column = store.getComponent(chunkRef, ChunkColumn.getComponentType());
        if (column == null) {
            return new FluidSample(0, (byte) 0);
        }
        int sectionIdx = ChunkUtil.chunkCoordinate(y);
        var sectionRef = column.getSection(sectionIdx);
        if (sectionRef == null || !sectionRef.isValid()) {
            return new FluidSample(0, (byte) 0);
        }
        FluidSection fluidSection = store.getComponent(sectionRef, FluidSection.getComponentType());
        if (fluidSection == null) {
            return new FluidSample(0, (byte) 0);
        }
        int fluidId = fluidSection.getFluidId(x, y, z);
        byte level = fluidSection.getFluidLevel(x, y, z);
        return new FluidSample(fluidId, level);
    }
}
