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

/** Modifie le poids d'une entrée (index 1-based, poids 1–1000). */
final class HyzaliaPasteWeightCommand extends AbstractPlayerCommand {

    private final RequiredArg<Integer> indexArg;
    private final RequiredArg<Integer> weightArg;

    HyzaliaPasteWeightCommand() {
        super("weight", "server.hyzalia.paint.paste.weight.desc");
        setPermissionGroups("hytale:WorldEditor");
        indexArg = withRequiredArg("index", "server.hyzalia.paint.paste.weight.desc.index", ArgTypes.INTEGER);
        weightArg = withRequiredArg("weight", "server.hyzalia.paint.paste.weight.desc.weight", ArgTypes.INTEGER);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        int index = context.get(indexArg) - 1;
        int weight = context.get(weightArg);
        if (weight < 1 || weight > 1000) {
            context.sendMessage(Message.translation("server.hyzalia.paint.paste.invalidWeight"));
            return;
        }
        MultiPrefabPasteState state = MultiPrefabPasteState.ensure(ref, store);
        if (index < 0 || index >= state.size()) {
            context.sendMessage(Message.translation("server.hyzalia.paint.paste.invalidIndex"));
            return;
        }
        state.setWeightAt(index, weight);
        WeightedPrefabEntry entry = state.entriesView().get(index);
        context.sendMessage(Message.translation("server.hyzalia.paint.paste.weightSet")
                .param("name", entry.displayName())
                .param("weight", weight));
    }
}
