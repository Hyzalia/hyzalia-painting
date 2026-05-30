package com.hypixel.hytale.builtin.buildertools.prefablist;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolPrefabPreview;
import com.hypixel.hytale.protocol.packets.interface_.EditorBlocksChange;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Envoi du preview 3D prefab (logique alignée sur {@link PrefabPage}). */
public final class PrefabPreviewSender {

    private static final int DEFAULT_TILT = 23;
    private static final int DEFAULT_SPIN_SPEED = 27;
    private static final int DEFAULT_PREVIEW_SCALE = 100;
    private static final int DEFAULT_BIOME_TINT = 0xFFFFFFFF;
    private static final int DEFAULT_WATER_TINT = 0xFFFFFFFF;

    private PrefabPreviewSender() {
    }

    public static void sendPreview(
            @Nonnull PlayerRef playerRef,
            @Nullable BlockSelection selection) {
        BuilderToolPrefabPreview packet = new BuilderToolPrefabPreview();
        if (selection != null) {
            packet.tilt = DEFAULT_TILT;
            packet.spinSpeed = DEFAULT_SPIN_SPEED;
            packet.previewScale = DEFAULT_PREVIEW_SCALE;
            EditorBlocksChange change = selection.toPacket();
            packet.blocksChange = change.blocksChange;
            packet.fluidsChange = change.fluidsChange;
            packet.entityChanges = change.entityChanges;
            applyTintFromPlayerPosition(playerRef, packet);
        }
        playerRef.getPacketHandler().write(packet);
    }

    public static void clearPreview(@Nonnull PlayerRef playerRef) {
        sendPreview(playerRef, null);
    }

    private static void applyTintFromPlayerPosition(
            @Nonnull PlayerRef playerRef,
            @Nonnull BuilderToolPrefabPreview packet) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            packet.biomeTint = DEFAULT_BIOME_TINT;
            packet.waterTint = DEFAULT_WATER_TINT;
            return;
        }
        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            packet.biomeTint = DEFAULT_BIOME_TINT;
            packet.waterTint = DEFAULT_WATER_TINT;
            return;
        }

        World world = store.getExternalData().getWorld();
        var position = playerRef.getTransform().getPosition();
        int blockX = MathUtil.floor(position.x);
        int blockY = MathUtil.floor(position.y);
        int blockZ = MathUtil.floor(position.z);
        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockX, blockZ);
        WorldChunk worldChunk = world.getNonTickingChunk(chunkIndex);
        if (worldChunk == null || worldChunk.getBlockChunk() == null) {
            packet.biomeTint = DEFAULT_BIOME_TINT;
            packet.waterTint = DEFAULT_WATER_TINT;
            return;
        }

        BlockChunk blockChunk = worldChunk.getBlockChunk();
        packet.biomeTint = blockChunk.getTint(blockX, blockZ);

        int environmentId = blockChunk.getEnvironment(blockX, blockY, blockZ);
        Environment environment = Environment.getAssetMap().getAsset(environmentId);
        if (environment != null && environment.getWaterTint() != null) {
            var tint = environment.getWaterTint();
            packet.waterTint = (tint.red & 0xFF) << 16 | (tint.green & 0xFF) << 8 | (tint.blue & 0xFF);
            return;
        }
        packet.waterTint = DEFAULT_WATER_TINT;
    }
}
