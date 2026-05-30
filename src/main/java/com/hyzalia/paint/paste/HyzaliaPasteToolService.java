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
import com.hypixel.hytale.server.core.prefab.PrefabLoadException;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.prefab.selection.standard.RotateBlockMode;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

/** Logique paste : sélection pondérée, chargement clipboard, rotation aléatoire, délégation vanilla. */
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
            WeightedPrefabEntry picked = pickEntry(player, playerRef, state);
            if (picked == null) {
                return;
            }
            if (!loadIntoBuilderState(playerRef, builderState, picked, queueAccessor)) {
                return;
            }
            if (tool != null && stack != null) {
                applyToolRandomizeIfEnabled(entityRef, builderState, tool, stack, queueAccessor);
            }
            builderState.paste(entityRef, x, y, z, false, !pasteAir, queueAccessor);
        });
    }

    /** Charge la prefab sélectionnée dans le clipboard builder (preview en jeu). */
    public static void syncPreviewClipboard(
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull MultiPrefabPasteState state,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (!HyzaliaPasteHeldTool.getHeld(ref, accessor).isPresent()) {
            return;
        }
        WeightedPrefabEntry preview = state.selectedEntry();
        if (preview == null) {
            return;
        }
        WeightedPrefabEntry entry = preview;
        BuilderToolsPlugin.addToQueue(
                player,
                playerRef,
                (entityRef, builderState, queueAccessor) ->
                        loadIntoBuilderState(playerRef, builderState, entry, queueAccessor));
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
            WeightedPrefabEntry picked = pickEntry(player, playerRef, state);
            if (picked == null) {
                return;
            }
            if (!loadIntoBuilderState(playerRef, builderState, picked, queueAccessor)) {
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

    private static void applyToolRandomizeIfEnabled(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull BuilderToolsPlugin.BuilderState builderState,
            @Nonnull BuilderTool tool,
            @Nonnull ItemStack stack,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        String randomize = HyzaliaPasteToolArgs.randomizeRotation(tool, stack);
        if ("No".equalsIgnoreCase(randomize)) {
            return;
        }

        Random random = builderState.getRandom();
        int deltaY = 0;
        int deltaX = 0;
        int deltaZ = 0;
        if ("RandomY".equalsIgnoreCase(randomize) || "RandomXYZ".equalsIgnoreCase(randomize)) {
            deltaY = pickRightAngle(random);
        }
        if ("RandomXYZ".equalsIgnoreCase(randomize)) {
            deltaX = pickRightAngle(random);
            deltaZ = pickRightAngle(random);
        }

        boolean randomFlip = HyzaliaPasteToolArgs.randomFlip(tool, stack);
        RotateBlockMode rotateBlockMode = HyzaliaPasteToolArgs.rotateBlockMode(tool, stack);
        builderState.applyRandomizeTransforms(
                ref,
                deltaX,
                deltaY,
                deltaZ,
                randomFlip,
                randomFlip,
                randomFlip,
                rotateBlockMode,
                accessor);
    }

    @Nullable
    private static WeightedPrefabEntry pickEntry(
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull MultiPrefabPasteState state) {
        WeightedPrefabEntry picked = HyzaliaPastePrefabLoader.pickWeighted(
                state.entriesView(),
                BuilderToolsPlugin.getState(player, playerRef).getRandom());
        if (picked == null) {
            playerRef.sendMessage(Message.translation("server.hyzalia.paint.paste.emptyList"));
        }
        return picked;
    }

    private static int pickRightAngle(@Nonnull Random random) {
        return random.nextInt(4) * 90;
    }

    private static boolean loadIntoBuilderState(
            @Nonnull PlayerRef playerRef,
            @Nonnull BuilderToolsPlugin.BuilderState builderState,
            @Nonnull WeightedPrefabEntry entry,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        try {
            BlockSelection selection = HyzaliaPastePrefabLoader.loadSelection(entry);
            builderState.load(entry.displayName(), selection, accessor);
            return true;
        } catch (PrefabLoadException ex) {
            playerRef.sendMessage(Message.translation("server.hyzalia.paint.paste.prefabNotFound")
                    .param("name", entry.displayName())
                    .param("path", entry.prefabPath()));
            return false;
        }
    }
}
