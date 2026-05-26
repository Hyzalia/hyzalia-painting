package com.hyzalia.paint.commands.stratum;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.asset.type.buildertool.config.args.ToolArgException;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hyzalia.paint.tools.LayersStratumToolArgs;

import javax.annotation.Nonnull;
import java.util.Objects;

abstract class StratumYCommands {

    private StratumYCommands() {
    }

    static int resolveY(
            CommandContext context,
            OptionalArg<Integer> yArg,
            Ref<EntityStore> ref,
            Store<EntityStore> store) {
        if (yArg.provided(context)) {
            return LayersStratumToolArgs.clampY(yArg.get(context));
        }
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return 0;
        }
        return LayersStratumToolArgs.clampY(MathUtil.floor(transform.getPosition().y()));
    }

    static void applyY(
            CommandContext context,
            Ref<EntityStore> ref,
            Store<EntityStore> store,
            boolean top,
            int y) {
        LayersStratumToolArgs.HeldLayersStratum held = LayersStratumToolArgs.getHeld(ref, store);
        if (held == null) {
            context.sendMessage(Message.translation("server.hyzalia.paint.stratum.noTool"));
            return;
        }

        LayersStratumToolArgs.Bounds bounds = LayersStratumToolArgs.readBounds(held.builderTool(), held.itemStack());
        int topY = top ? y : bounds.topY();
        int bottomY = top ? bounds.bottomY() : y;
        int normalizedTop = LayersStratumToolArgs.normalizeTop(topY, bottomY);
        int normalizedBottom = LayersStratumToolArgs.normalizeBottom(topY, bottomY);
        boolean swapped = normalizedTop != topY || normalizedBottom != bottomY;

        try {
            LayersStratumToolArgs.writeBounds(held, normalizedTop, normalizedBottom, true);
        } catch (ToolArgException ex) {
            context.sendMessage(ex.getTranslationMessage());
            return;
        }

        Message message = Message.translation(
                        top ? "server.hyzalia.paint.stratum.topSet" : "server.hyzalia.paint.stratum.bottomSet")
                .param("y", String.valueOf(top ? normalizedTop : normalizedBottom))
                .param("top", String.valueOf(normalizedTop))
                .param("bottom", String.valueOf(normalizedBottom));
        context.sendMessage(message);
        if (swapped) {
            context.sendMessage(Message.translation("server.hyzalia.paint.stratum.boundsSwapped")
                    .param("top", String.valueOf(normalizedTop))
                    .param("bottom", String.valueOf(normalizedBottom)));
        }
    }

    static abstract class AbstractStratumYCommand extends AbstractPlayerCommand {

        private final OptionalArg<Integer> yArg;

        protected AbstractStratumYCommand(@Nonnull String name, @Nonnull String descriptionKey) {
            super(name, descriptionKey);
            requirePermission(HytalePermissions.EDITOR_BRUSH_USE);
            yArg = withOptionalArg("y", descriptionKey + ".y", ArgTypes.INTEGER);
        }

        protected OptionalArg<Integer> yArg() {
            return yArg;
        }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world) {
            Objects.requireNonNull(
                    store.getComponent(ref, Player.getComponentType()),
                    "Player component");
            applyY(context, ref, store, setsTop(), resolveY(context, yArg, ref, store));
        }

        protected abstract boolean setsTop();
    }
}
