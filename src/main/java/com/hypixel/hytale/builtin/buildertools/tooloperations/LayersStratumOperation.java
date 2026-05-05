package com.hypixel.hytale.builtin.buildertools.tooloperations;

import com.hyzalia.paint.tools.LayerSpec;
import com.hyzalia.paint.tools.LayersGradientBuilder;
import com.hypixel.hytale.builtin.buildertools.tooloperations.ToolOperation;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolOnUseInteraction;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockPattern;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import it.unimi.dsi.fastutil.Pair;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Brush « LayersStratum » du plugin Hyzalia Painting.
 *
 * <p>Deux modes :
 * <ul>
 *   <li><strong>Surface</strong> (défaut) : colonnes depuis chaque bloc touché, épaisseur = rangées ;</li>
 *   <li><strong>Strate monde</strong> : bande Y fixe {@code [zbStratumBottomY, zaStratumTopY]} ; les
 *       épaisseurs sont des <em>poids</em> répartis sur la hauteur de bande (méthode du plus grand reste),
 *       puis dégradés RadUp/RadDown ; un seul bloc est peint par {@code execute0} selon {@code y}.</li>
 * </ul>
 *
 * <p>Extensions RadUp / RadDown hors bande : en strate monde, déclenchées une fois par colonne
 * lorsque le brush touche exactement la rangée du haut ou du bas de la bande.
 */
public class LayersStratumOperation extends ToolOperation {

    private final Vector3i depthDirection;
    private final int brushDensity;
    private final List<LayerSpec> activeLayers;

    private final boolean worldStratum;
    private final int stratumTopY;
    private final int stratumBottomY;
    private final int bandHeight;

    public LayersStratumOperation(
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull Player player,
            @Nonnull BuilderToolOnUseInteraction interaction,
            @Nonnull ComponentAccessor<EntityStore> components) {
        super(playerRef, interaction, components);

        Map<String, Object> tool = this.args.tool();

        HeadRotation headRotation = components.getComponent(playerRef, HeadRotation.getComponentType());
        this.depthDirection = resolveDirection(tool, headRotation);
        this.brushDensity = readInt(tool, "sBrushDensity", 100);

        this.worldStratum = readBool(tool, "zUseWorldStratum", false);
        int topY = readInt(tool, "zaStratumTopY", 255);
        int bottomY = readInt(tool, "zbStratumBottomY", -64);
        if (topY < bottomY) {
            int swap = topY;
            topY = bottomY;
            bottomY = swap;
        }
        this.stratumTopY = topY;
        this.stratumBottomY = bottomY;
        this.bandHeight = topY - bottomY + 1;

        List<LayerSpec> layers = new ArrayList<>(6);
        layers.add(readLayer(tool,
                "bLayerOneLength", "bLayerRadUpLength", "bLayerRadUpChance",
                "bLayerRadDownLength", "bLayerRadDownChance",
                "cLayerOneMaterial", null, "tSkipLayerOne"));
        layers.add(readLayer(tool,
                "eLayerTwoLength", "eLayerRadUpLength", "eLayerRadUpChance",
                "eLayerRadDownLength", "eLayerRadDownChance",
                "fLayerTwoMaterial", "dEnableLayerTwo", "uSkipLayerTwo"));
        layers.add(readLayer(tool,
                "hLayerThreeLength", "hLayerRadUpLength", "hLayerRadUpChance",
                "hLayerRadDownLength", "hLayerRadDownChance",
                "iLayerThreeMaterial", "gEnableLayerThree", "vSkipLayerThree"));
        layers.add(readLayer(tool,
                "kLayerFourLength", "kLayerRadUpLength", "kLayerRadUpChance",
                "kLayerRadDownLength", "kLayerRadDownChance",
                "lLayerFourMaterial", "jEnableLayerFour", "wSkipLayerFour"));
        layers.add(readLayer(tool,
                "nLayerFiveLength", "nLayerRadUpLength", "nLayerRadUpChance",
                "nLayerRadDownLength", "nLayerRadDownChance",
                "oLayerFiveMaterial", "mEnableLayerFive", "xSkipLayerFive"));
        layers.add(readLayer(tool,
                "qLayerSixLength", "qLayerRadUpLength", "qLayerRadUpChance",
                "qLayerRadDownLength", "qLayerRadDownChance",
                "rLayerSixMaterial", "pEnableLayerSix", "ySkipLayerSix"));

        List<LayerSpec> active = new ArrayList<>(layers.size());
        for (LayerSpec layer : layers) {
            if (layer.active()) {
                active.add(layer);
            }
        }
        this.activeLayers = active;
    }

    /**
     * Visibilité {@code public} : compatible parent {@code package-private} / {@code protected}
     * / {@code public} (élargissement). Même package que {@link ToolOperation} — le contrat
     * d’extension BuilderTools est déjà satisfait ; un early-plugin qui modifie {@code ToolOperation}
     * ne corrige pas un {@link java.lang.AbstractMethodError} dû à deux définitions de classe
     * (class loaders différents).
     */
    @Override
    public boolean execute0(int x, int y, int z) {
        if (this.edit.getBlock(x, y, z) <= 0) {
            return true;
        }
        if (this.random.nextInt(100) > this.brushDensity - 1) {
            return true;
        }
        if (this.activeLayers.isEmpty()) {
            return true;
        }

        if (this.worldStratum && this.bandHeight > 0) {
            return execute0WorldStratum(x, y, z);
        }

        List<String> rowMaterials = LayersGradientBuilder.buildColumn(this.activeLayers, this.random);
        if (rowMaterials.isEmpty()) {
            return true;
        }

        List<Pair<Integer, String>> pairs = new ArrayList<>(rowMaterials.size());
        for (String material : rowMaterials) {
            pairs.add(Pair.of(Integer.valueOf(1), material));
        }

        WorldChunk chunk = (WorldChunk) this.edit.getAccessor()
                .getChunk(ChunkUtil.indexChunkFromBlock(x, z));

        this.builderState.layer(
                x, y, z,
                pairs, pairs.size(),
                this.depthDirection,
                chunk,
                this.edit.getBefore(),
                this.edit.getAfter());

        int dx = this.depthDirection.getX();
        int dy = this.depthDirection.getY();
        int dz = this.depthDirection.getZ();
        int upX = -dx;
        int upY = -dy;
        int upZ = -dz;

        boolean isTopOfColumn = this.edit.getBlock(x + upX, y + upY, z + upZ) <= 0;
        if (isTopOfColumn) {
            LayerSpec first = this.activeLayers.get(0);
            if (first.radUpRows() > 0) {
                placeExtension(
                        x + upX, y + upY, z + upZ,
                        upX, upY, upZ,
                        first, first.radUpRows(), first.radUpChance());
            }

            LayerSpec last = this.activeLayers.get(this.activeLayers.size() - 1);
            if (last.radDownRows() > 0) {
                int total = pairs.size();
                placeExtension(
                        x + dx * total, y + dy * total, z + dz * total,
                        dx, dy, dz,
                        last, last.radDownRows(), last.radDownChance());
            }
        }

        return true;
    }

    /**
     * Peint un bloc dans la bande Y monde : la rangée {@code stratumTopY - y} reçoit le matériau
     * issu de la colonne virtuelle de hauteur {@link #bandHeight} (poids + dégradés).
     */
    private boolean execute0WorldStratum(int x, int y, int z) {
        if (y < this.stratumBottomY || y > this.stratumTopY) {
            return true;
        }

        List<String> column = LayersGradientBuilder.buildColumnFromWeights(
                this.activeLayers, this.bandHeight, this.random);
        if (column.isEmpty()) {
            return true;
        }
        int rowFromTop = this.stratumTopY - y;
        if (rowFromTop < 0 || rowFromTop >= column.size()) {
            return true;
        }
        String material = column.get(rowFromTop);
        placeMaterialAtBlock(x, y, z, material);

        int dx = this.depthDirection.getX();
        int dy = this.depthDirection.getY();
        int dz = this.depthDirection.getZ();
        int upX = -dx;
        int upY = -dy;
        int upZ = -dz;

        if (y == this.stratumTopY) {
            LayerSpec first = this.activeLayers.get(0);
            if (first.radUpRows() > 0) {
                placeExtension(
                        x + upX, this.stratumTopY + upY, z + upZ,
                        upX, upY, upZ,
                        first, first.radUpRows(), first.radUpChance());
            }
        }
        if (y == this.stratumBottomY) {
            LayerSpec last = this.activeLayers.get(this.activeLayers.size() - 1);
            if (last.radDownRows() > 0) {
                placeExtension(
                        x + dx, this.stratumBottomY + dy, z + dz,
                        dx, dy, dz,
                        last, last.radDownRows(), last.radDownChance());
            }
        }

        return true;
    }

    private void placeMaterialAtBlock(int bx, int by, int bz, String material) {
        if (material == null || material.isEmpty()) {
            return;
        }
        BlockPattern pattern = BlockPattern.parse(material);
        if (pattern == null || pattern.isEmpty() || pattern.hasInvalidBlocks()) {
            return;
        }
        pattern.resolve();
        int blockId = pattern.nextBlock(this.random);
        if (blockId <= 0) {
            return;
        }
        this.edit.setBlock(bx, by, bz, blockId);
    }

    private void placeExtension(
            int firstX, int firstY, int firstZ,
            int stepX, int stepY, int stepZ,
            LayerSpec layer, int rows, int chance) {
        String material = layer.material();
        if (material == null || material.isEmpty()) {
            return;
        }
        BlockPattern pattern = BlockPattern.parse(material);
        if (pattern == null || pattern.isEmpty() || pattern.hasInvalidBlocks()) {
            return;
        }
        pattern.resolve();
        boolean[] mask = LayersGradientBuilder.rollExtensionMask(rows, chance, this.random);
        for (int i = 0; i < rows; i++) {
            if (!mask[i]) {
                continue;
            }
            int blockId = pattern.nextBlock(this.random);
            if (blockId <= 0) {
                continue;
            }
            this.edit.setBlock(
                    firstX + stepX * i,
                    firstY + stepY * i,
                    firstZ + stepZ * i,
                    blockId);
        }
    }

    private static LayerSpec readLayer(
            Map<String, Object> tool,
            String thicknessKey,
            String radUpKey,
            String radUpChanceKey,
            String radDownKey,
            String radDownChanceKey,
            String materialKey,
            String enableKey,
            String skipKey) {
        String material = readString(tool, materialKey, "Empty");
        int thickness = readInt(tool, thicknessKey, 0);
        int radUp = readInt(tool, radUpKey, 0);
        int radUpChance = readInt(tool, radUpChanceKey, 50);
        int radDown = readInt(tool, radDownKey, 0);
        int radDownChance = readInt(tool, radDownChanceKey, 50);
        boolean enabled = enableKey == null
                ? true
                : readBool(tool, enableKey, true);
        boolean skip = readBool(tool, skipKey, false);
        return new LayerSpec(
                material, thickness,
                radUp, radUpChance,
                radDown, radDownChance,
                enabled, skip);
    }

    private static Vector3i resolveDirection(Map<String, Object> tool, HeadRotation headRotation) {
        String direction = readString(tool, "aDirection", "Down");
        switch (direction) {
            case "Up":
                return Vector3i.UP;
            case "Down":
                return Vector3i.DOWN;
            case "North":
                return Vector3i.NORTH;
            case "South":
                return Vector3i.SOUTH;
            case "East":
                return Vector3i.EAST;
            case "West":
                return Vector3i.WEST;
            case "Camera":
                return headRotation != null
                        ? headRotation.getAxisDirection()
                        : Vector3i.DOWN;
            default:
                return Vector3i.DOWN;
        }
    }

    private static int readInt(Map<String, Object> tool, String key, int defaultValue) {
        Object raw = tool.get(key);
        if (raw instanceof Integer integer) {
            return integer.intValue();
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    private static boolean readBool(Map<String, Object> tool, String key, boolean defaultValue) {
        Object raw = tool.get(key);
        if (raw instanceof Boolean bool) {
            return bool.booleanValue();
        }
        return defaultValue;
    }

    private static String readString(Map<String, Object> tool, String key, String defaultValue) {
        Object raw = tool.get(key);
        if (raw == null) {
            return defaultValue;
        }
        return raw.toString();
    }
}
