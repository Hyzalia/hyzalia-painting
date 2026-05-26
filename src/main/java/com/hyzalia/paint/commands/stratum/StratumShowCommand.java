package com.hyzalia.paint.commands.stratum;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hyzalia.paint.tools.LayersStratumToolArgs;

import javax.annotation.Nonnull;

/** {@code /stratum show} — affiche les bornes Y et le mode strate monde du brush tenu en main. */
public final class StratumShowCommand extends AbstractPlayerCommand {

    public StratumShowCommand() {
        super("show", "server.hyzalia.paint.stratum.show.desc");
        requirePermission(HytalePermissions.EDITOR_BRUSH_USE);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        LayersStratumToolArgs.HeldLayersStratum held = LayersStratumToolArgs.getHeld(ref, store);
        if (held == null) {
            context.sendMessage(Message.translation("server.hyzalia.paint.stratum.noTool"));
            return;
        }

        LayersStratumToolArgs.Bounds bounds =
                LayersStratumToolArgs.readBounds(held.builderTool(), held.itemStack());
        context.sendMessage(Message.translation("server.hyzalia.paint.stratum.show")
                .param("top", String.valueOf(bounds.topY()))
                .param("bottom", String.valueOf(bounds.bottomY()))
                .param("height", String.valueOf(bounds.topY() - bounds.bottomY() + 1))
                .param("worldStratum", bounds.worldStratum() ? "on" : "off"));
    }
}
