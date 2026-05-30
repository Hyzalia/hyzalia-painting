package com.hyzalia.paint.commands.paste;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hyzalia.paint.paste.MultiPrefabPasteState;
import com.hyzalia.paint.paste.WeightedPrefabEntry;

import javax.annotation.Nonnull;

/** Retire une prefab de la liste par index (1-based). */
final class HyzaliaPasteRemoveCommand extends AbstractPlayerCommand {

    private final RequiredArg<Integer> indexArg;

    HyzaliaPasteRemoveCommand() {
        super("remove", "server.hyzalia.paint.paste.remove.desc");
        setPermissionGroups("hytale:WorldEditor");
        indexArg = withRequiredArg("index", "server.hyzalia.paint.paste.remove.desc.index", ArgTypes.INTEGER);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        int index = context.get(indexArg) - 1;
        MultiPrefabPasteState state = MultiPrefabPasteState.ensure(ref, store);
        if (index < 0 || index >= state.size()) {
            context.sendMessage(Message.translation("server.hyzalia.paint.paste.invalidIndex"));
            return;
        }
        WeightedPrefabEntry removed = state.entriesView().get(index);
        state.removeAt(index);
        context.sendMessage(Message.translation("server.hyzalia.paint.paste.removed")
                .param("name", removed.displayName()));
    }
}
