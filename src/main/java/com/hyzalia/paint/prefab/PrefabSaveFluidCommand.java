package com.hyzalia.paint.prefab;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Ouvre le même écran que {@code /prefab save} vanilla, avec réécriture fluides après sauvegarde.
 */
public final class PrefabSaveFluidCommand extends AbstractPlayerCommand {

    private static final Message MESSAGE_NO_SELECTION =
            Message.translation("server.builderTools.noSelection");

    public PrefabSaveFluidCommand() {
        super("prefabsavefluid", "server.hyzalia.paint.prefabsavefluid.desc");
        requirePermission("hytale.editor.prefab.manage");
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = Objects.requireNonNull(
                store.getComponent(ref, Player.getComponentType()),
                "Player component");

        BuilderToolsPlugin.BuilderState builderState = BuilderToolsPlugin.getState(player, playerRef);
        BlockSelection selection = builderState.getSelection();
        if (selection == null || !selection.hasSelectionBounds()) {
            context.sendMessage(MESSAGE_NO_SELECTION);
            return;
        }

        PageManager pm = player.getPageManager();
        pm.openCustomPage(ref, store, new FluidAwarePrefabSavePage(playerRef));
    }
}
