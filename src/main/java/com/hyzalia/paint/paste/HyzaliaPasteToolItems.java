package com.hyzalia.paint.paste;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Utilitaires pour détecter l'outil Hyzalia Paste en main. */
public final class HyzaliaPasteToolItems {

    private HyzaliaPasteToolItems() {
    }

    public static boolean isHyzaliaPasteItem(@Nullable ItemStack stack) {
        return stack != null && HyzaliaPasteToolConstants.ITEM_ID.equals(stack.getItemId());
    }

    public static boolean isHoldingHyzaliaPasteTool(@Nonnull PlayerRef playerRef) {
        Holder<EntityStore> holder = playerRef.getHolder();
        if (holder == null) {
            return false;
        }
        return isHyzaliaPasteItem(InventoryComponent.getItemInHand(holder));
    }

    public static boolean isHoldingHyzaliaPasteTool(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        ItemStack stack = InventoryComponent.getItemInHand(accessor, ref);
        return isHyzaliaPasteItem(stack);
    }
}
