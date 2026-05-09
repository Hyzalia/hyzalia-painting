package com.hyzalia.paint.prefab;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.builtin.buildertools.BuilderToolsUserData;
import com.hypixel.hytale.builtin.buildertools.prefablist.PrefabSavePage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.ui.browser.AssetPackSaveBrowser;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Même UI que {@link PrefabSavePage} (héritage : {@code build()} inchangé), mais la sauvegarde depuis la sélection
 * applique un enrichissement fluides après la passe vanilla (anchor bloc ou joueur).
 */
public final class FluidAwarePrefabSavePage extends PrefabSavePage {

    private static final Message MESSAGE_NAME_REQUIRED =
            Message.translation("server.builderTools.prefabSave.nameRequired");
    private static final Message MESSAGE_PACK_REQUIRED =
            Message.translation("server.customUI.assetPackBrowser.packRequired");

    public FluidAwarePrefabSavePage(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    public void handleDataEvent(
            Ref<EntityStore> ref,
            Store<EntityStore> store,
            PrefabSavePage.PageData pageData) {
        Player player = Objects.requireNonNull(
                store.getComponent(ref, Player.getComponentType()),
                "Player component");

        AssetPackSaveBrowser packBrowser = PrefabSavePageAccess.packBrowser(this);
        String actionKey = pageData.action != null ? pageData.action.name() : null;
        AssetPackSaveBrowser.ActionResult browserResult = packBrowser.handleAction(
                "#MainPage #SelectedPackLabel",
                pageData.packBrowserData,
                actionKey);

        if (browserResult != null) {
            if (browserResult.errorKey() != null) {
                playerRef.sendMessage(Message.translation(browserResult.errorKey()));
            }
            if (browserResult.packConfirmed() && packBrowser.hasSelectedPack()) {
                BuilderToolsUserData.get(player).setLastSavePack(packBrowser.getSelectedPack().getName());
            }
            sendUpdate(browserResult.commandBuilder(), browserResult.eventBuilder(), false);
            return;
        }

        if (pageData.action == null) {
            return;
        }

        if (pageData.action == PrefabSavePage.Action.Save) {
            handleSave(ref, store, player, pageData, packBrowser);
        } else if (pageData.action == PrefabSavePage.Action.Cancel) {
            closePage(player, ref, store);
        }
    }

    private void handleSave(
            Ref<EntityStore> ref,
            Store<EntityStore> store,
            Player player,
            PrefabSavePage.PageData pageData,
            AssetPackSaveBrowser packBrowser) {
        if (pageData.name == null || pageData.name.isBlank()) {
            playerRef.sendMessage(MESSAGE_NAME_REQUIRED);
            sendUpdate(null, null, false);
            return;
        }
        var selectedPack = packBrowser.getSelectedPack();
        if (selectedPack == null) {
            playerRef.sendMessage(MESSAGE_PACK_REQUIRED);
            sendUpdate(null, null, false);
            return;
        }

        BuilderToolsUserData.get(player).setLastSavePack(selectedPack.getName());

        closePage(player, ref, store);

        boolean useAnchor = pageData.usePlayerAnchor && !pageData.fromClipboard;
        var anchorWorld = FluidInclusivePrefabSave.computePlayerAnchorWorld(ref, store, useAnchor, pageData.fromClipboard);

        BuilderToolsPlugin.addToQueue(player, playerRef, (entityRef, builderState, accessor) ->
                FluidInclusivePrefabSave.handle(
                        pageData.fromClipboard,
                        pageData.name,
                        pageData.overwrite,
                        pageData.entities,
                        pageData.empty,
                        pageData.clearSupport,
                        anchorWorld,
                        selectedPack,
                        entityRef,
                        builderState,
                        accessor));
    }

    private static void closePage(Player player, Ref<EntityStore> ref, Store<EntityStore> store) {
        PageManager pm = player.getPageManager();
        pm.setPage(ref, store, Page.None);
    }
}
