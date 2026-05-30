package com.hyzalia.paint.commands.paste;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.builtin.buildertools.prefablist.HyzaliaMultiPrefabPastePage;

import javax.annotation.Nonnull;
import java.util.Objects;

/** Ouvre le navigateur prefab pour ajouter une entrée pondérée. */
final class HyzaliaPasteAddCommand extends AbstractPlayerCommand {

    HyzaliaPasteAddCommand() {
        super("add", "server.hyzalia.paint.paste.add.desc");
        addAliases("load");
        setPermissionGroups("hytale:WorldEditor");
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
        PageManager pageManager = player.getPageManager();
        pageManager.openCustomPage(ref, store, new HyzaliaMultiPrefabPastePage(playerRef, builderState));
    }
}
