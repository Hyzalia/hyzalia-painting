package com.hyzalia.paint.commands.paste;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hyzalia.paint.paste.MultiPrefabListPage;
import com.hyzalia.paint.paste.MultiPrefabPasteState;
import com.hyzalia.paint.paste.WeightedPrefabEntry;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

/** Affiche la liste ou ouvre l'UI de gestion. */
final class HyzaliaPasteListCommand extends AbstractPlayerCommand {

    private final boolean openUi;

    HyzaliaPasteListCommand(String name, String descriptionKey, boolean openUi) {
        super(name, descriptionKey);
        this.openUi = openUi;
        setPermissionGroups("hytale:WorldEditor");
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        MultiPrefabPasteState state = MultiPrefabPasteState.ensure(ref, store);
        if (openUi) {
            Player player = Objects.requireNonNull(
                    store.getComponent(ref, Player.getComponentType()),
                    "Player component");
            PageManager pageManager = player.getPageManager();
            pageManager.openCustomPage(ref, store, new MultiPrefabListPage(playerRef));
            return;
        }

        List<WeightedPrefabEntry> entries = state.entriesView();
        if (entries.isEmpty()) {
            context.sendMessage(Message.translation("server.hyzalia.paint.paste.emptyList"));
            return;
        }
        context.sendMessage(Message.translation("server.hyzalia.paint.paste.listHeader"));
        for (int i = 0; i < entries.size(); i++) {
            WeightedPrefabEntry entry = entries.get(i);
            context.sendMessage(Message.translation("server.hyzalia.paint.paste.listRow")
                    .param("index", i + 1)
                    .param("name", entry.displayName())
                    .param("weight", entry.weight()));
        }
    }
}
