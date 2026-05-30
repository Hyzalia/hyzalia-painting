package com.hyzalia.paint.paste;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.builtin.buildertools.prefablist.HyzaliaMultiPrefabPastePage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/** Ouverture des pages UI paste Hyzalia (Add Prefab, gestion du pool). */
public final class HyzaliaPasteUiActions {

    private HyzaliaPasteUiActions() {
    }

    public static void openAddPrefabPage(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        BuilderToolsPlugin.BuilderState builderState = BuilderToolsPlugin.getState(player, playerRef);
        PageManager pageManager = player.getPageManager();
        pageManager.openCustomPage(ref, store, new HyzaliaMultiPrefabPastePage(playerRef, builderState));
    }

    public static void openManagePoolPage(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        PageManager pageManager = player.getPageManager();
        pageManager.openCustomPage(ref, store, new MultiPrefabListPage(playerRef));
    }
}
