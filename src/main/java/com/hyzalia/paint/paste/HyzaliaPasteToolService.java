package com.hyzalia.paint.paste;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolRandomizeClipboard;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.buildertool.config.BuilderTool;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.prefab.selection.standard.RotateBlockMode;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Random;

/** Logique paste : sélection pondérée, clipboard sans historique, délégation vanilla à la pose. */
public final class HyzaliaPasteToolService {

    private HyzaliaPasteToolService() {
    }

    public static void pasteAt(
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull MultiPrefabPasteState state,
            int x,
            int y,
            int z,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (state.isEmpty()) {
            playerRef.sendMessage(Message.translation("server.hyzalia.paint.paste.emptyList"));
            return;
        }

        BuilderTool tool = BuilderTool.getActiveBuilderTool(ref, accessor);
        ItemStack stack = InventoryComponent.getItemInHand(accessor, ref);
        boolean pasteAir = tool != null && stack != null && HyzaliaPasteToolArgs.pasteAir(tool, stack);

        BuilderToolsPlugin.addToQueue(player, playerRef, (entityRef, builderState, queueAccessor) -> {
            Random random = builderState.getRandom();
            WeightedPrefabEntry picked = state.ensurePendingPasteEntry(random);
            if (picked == null) {
                return;
            }

            HyzaliaPasteClipboardHelper.ensurePendingClipboardLoaded(
                    builderState, state, tool, stack, random);
            if (builderState.getSelection() == null) {
                HyzaliaPasteClipboardHelper.loadPendingIntoClipboard(
                        builderState, picked, tool, stack, random);
            }
            if (builderState.getSelection() == null) {
                return;
            }

            state.takePendingPasteEntry();
            builderState.paste(entityRef, x, y, z, false, !pasteAir, queueAccessor);
            loadNextPendingPreview(playerRef, builderState, state, tool, stack, random);
        });
    }

    /** Charge le prochain prefab pondéré dans le clipboard (preview en jeu = prochaine pose). */
    public static void syncPreviewClipboard(
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull MultiPrefabPasteState state,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (!HyzaliaPasteHeldTool.getHeld(ref, accessor).isPresent()) {
            return;
        }
        if (state.isEmpty()) {
            return;
        }

        BuilderTool tool = BuilderTool.getActiveBuilderTool(ref, accessor);
        ItemStack stack = InventoryComponent.getItemInHand(accessor, ref);
        Random random = BuilderToolsPlugin.getState(player, playerRef).getRandom();
        WeightedPrefabEntry entry = state.ensurePendingPasteEntry(random);
        if (entry == null) {
            return;
        }

        BuilderToolsPlugin.addToQueue(
                player,
                playerRef,
                (entityRef, builderState, queueAccessor) ->
                        HyzaliaPasteClipboardHelper.loadPendingIntoClipboard(
                                builderState, entry, tool, stack, random));
    }

    public static void applyRandomizeFromPacket(
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull MultiPrefabPasteState state,
            @Nonnull BuilderToolRandomizeClipboard packet,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (state.isEmpty()) {
            playerRef.sendMessage(Message.translation("server.hyzalia.paint.paste.emptyList"));
            return;
        }

        BuilderTool tool = BuilderTool.getActiveBuilderTool(ref, accessor);
        ItemStack stack = InventoryComponent.getItemInHand(accessor, ref);
        RotateBlockMode rotateBlockMode = RotateBlockMode.ALL;
        if (tool != null && stack != null) {
            rotateBlockMode = HyzaliaPasteToolArgs.rotateBlockMode(tool, stack);
        }

        RotateBlockMode finalRotateBlockMode = rotateBlockMode;
        BuilderToolsPlugin.addToQueue(player, playerRef, (entityRef, builderState, queueAccessor) -> {
            Random random = builderState.getRandom();
            HyzaliaPasteClipboardHelper.ensurePendingClipboardLoaded(
                    builderState, state, tool, stack, random);
            if (builderState.getSelection() == null) {
                return;
            }
            builderState.applyRandomizeTransforms(
                    entityRef,
                    packet.deltaX,
                    packet.deltaY,
                    packet.deltaZ,
                    packet.flipX,
                    packet.flipY,
                    packet.flipZ,
                    finalRotateBlockMode,
                    queueAccessor);
        });
    }

    private static void loadNextPendingPreview(
            @Nonnull PlayerRef playerRef,
            @Nonnull BuilderToolsPlugin.BuilderState builderState,
            @Nonnull MultiPrefabPasteState state,
            BuilderTool tool,
            ItemStack stack,
            @Nonnull Random random) {
        WeightedPrefabEntry next = state.ensurePendingPasteEntry(random);
        if (next != null) {
            HyzaliaPasteClipboardHelper.loadPendingIntoClipboard(
                    builderState, next, tool, stack, random);
        }
    }
}
