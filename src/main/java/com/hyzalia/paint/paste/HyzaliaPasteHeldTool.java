package com.hyzalia.paint.paste;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.buildertool.config.BuilderTool;
import com.hypixel.hytale.server.core.inventory.ActiveSlotInventoryComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/** Outil Hyzalia Paste tenu en main (section inventaire + slot actif). */
public final class HyzaliaPasteHeldTool {

    public record Held(
            @Nonnull BuilderTool builderTool,
            @Nonnull ItemStack itemStack,
            @Nonnull ActiveSlotInventoryComponent inventorySection,
            byte activeSlot) {
    }

    private HyzaliaPasteHeldTool() {
    }

    @Nonnull
    public static Optional<Held> getHeld(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        InventoryComponent.Tool toolSection =
                accessor.getComponent(ref, InventoryComponent.Tool.getComponentType());
        if (toolSection != null && toolSection.isUsingToolsItem()) {
            Held held = fromSection(toolSection);
            if (held != null) {
                return Optional.of(held);
            }
        }
        InventoryComponent.Hotbar hotbar =
                accessor.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbar != null) {
            Held held = fromSection(hotbar);
            if (held != null) {
                return Optional.of(held);
            }
        }
        return Optional.empty();
    }

    @Nullable
    private static Held fromSection(ActiveSlotInventoryComponent section) {
        ItemStack stack = section.getActiveItem();
        if (!HyzaliaPasteToolItems.isHyzaliaPasteItem(stack)) {
            return null;
        }
        if (stack.getItem() == null) {
            return null;
        }
        BuilderTool builderTool = stack.getItem().getBuilderTool();
        if (builderTool == null || !HyzaliaPasteToolItems.isHyzaliaPasteItem(stack)) {
            return null;
        }
        return new Held(builderTool, stack, section, section.getActiveSlot());
    }
}
