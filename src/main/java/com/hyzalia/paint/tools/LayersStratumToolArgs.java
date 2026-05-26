package com.hyzalia.paint.tools;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.buildertool.config.BuilderTool;
import com.hypixel.hytale.server.core.asset.type.buildertool.config.args.ToolArgException;
import com.hypixel.hytale.server.core.inventory.ActiveSlotInventoryComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/** Lecture / écriture des args {@code LayersStratum} sur l’outil BuilderTools tenu en main. */
public final class LayersStratumToolArgs {

    public static final String TOOL_ID = "LayersStratum";
    public static final String ARG_WORLD_STRATUM = "zUseWorldStratum";
    public static final String ARG_TOP_Y = "zaStratumTopY";
    public static final String ARG_BOTTOM_Y = "zbStratumBottomY";

    public static final int Y_MIN = -512;
    public static final int Y_MAX = 512;

    public record Bounds(int topY, int bottomY, boolean worldStratum) {
    }

    public record HeldLayersStratum(
            @Nonnull BuilderTool builderTool,
            @Nonnull ItemStack itemStack,
            @Nonnull ActiveSlotInventoryComponent inventorySection,
            byte activeSlot) {
    }

    private LayersStratumToolArgs() {
    }

    @Nullable
    public static HeldLayersStratum getHeld(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        InventoryComponent.Tool toolSection =
                accessor.getComponent(ref, InventoryComponent.Tool.getComponentType());
        if (toolSection != null && toolSection.isUsingToolsItem()) {
            HeldLayersStratum held = fromSection(toolSection);
            if (held != null) {
                return held;
            }
        }
        InventoryComponent.Hotbar hotbar =
                accessor.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbar != null) {
            return fromSection(hotbar);
        }
        return null;
    }

    @Nullable
    private static HeldLayersStratum fromSection(ActiveSlotInventoryComponent section) {
        ItemStack stack = section.getActiveItem();
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        BuilderTool builderTool = stack.getItem().getBuilderTool();
        if (builderTool == null || !TOOL_ID.equals(builderTool.getId())) {
            return null;
        }
        return new HeldLayersStratum(builderTool, stack, section, section.getActiveSlot());
    }

    public static Bounds readBounds(@Nonnull BuilderTool builderTool, @Nonnull ItemStack stack) {
        Map<String, Object> tool = builderTool.getItemArgData(stack).tool();
        return new Bounds(
                readInt(tool, ARG_TOP_Y, 255),
                readInt(tool, ARG_BOTTOM_Y, -64),
                readBool(tool, ARG_WORLD_STRATUM, false));
    }

    public static void writeBounds(
            @Nonnull HeldLayersStratum held,
            int topY,
            int bottomY,
            boolean worldStratum) throws ToolArgException {
        ItemStack stack = held.itemStack();
        stack = held.builderTool().updateArgMetadata(stack, ARG_TOP_Y, Integer.toString(topY));
        stack = held.builderTool().updateArgMetadata(stack, ARG_BOTTOM_Y, Integer.toString(bottomY));
        stack = held.builderTool().updateArgMetadata(stack, ARG_WORLD_STRATUM, Boolean.toString(worldStratum));
        held.inventorySection().getInventory().setItemStackForSlot(held.activeSlot(), stack);
    }

    public static int clampY(int y) {
        return Math.max(Y_MIN, Math.min(Y_MAX, y));
    }

    public static int normalizeTop(int topY, int bottomY) {
        return Math.max(topY, bottomY);
    }

    public static int normalizeBottom(int topY, int bottomY) {
        return Math.min(topY, bottomY);
    }

    private static int readInt(Map<String, Object> tool, String key, int defaultValue) {
        Object raw = tool.get(key);
        if (raw instanceof Integer integer) {
            return integer;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    private static boolean readBool(Map<String, Object> tool, String key, boolean defaultValue) {
        Object raw = tool.get(key);
        if (raw instanceof Boolean bool) {
            return bool;
        }
        return defaultValue;
    }
}
