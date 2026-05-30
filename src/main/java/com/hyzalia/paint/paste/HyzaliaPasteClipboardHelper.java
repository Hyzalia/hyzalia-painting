package com.hyzalia.paint.paste;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.Axis;
import com.hypixel.hytale.server.core.asset.type.buildertool.config.BuilderTool;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.prefab.selection.standard.RotateBlockMode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

/** Mise à jour du clipboard builder sans entrée d'historique (pas de {@code BuilderState#load}). */
final class HyzaliaPasteClipboardHelper {

    private HyzaliaPasteClipboardHelper() {
    }

    static void setClipboardWithoutHistory(
            @Nonnull BuilderToolsPlugin.BuilderState builderState,
            @Nonnull BlockSelection selection) {
        builderState.setSelection(selection.cloneSelection());
        builderState.syncRawPositions();
        builderState.sendSelectionToClient();
    }

    @Nonnull
    static BlockSelection prepareSelection(
            @Nonnull WeightedPrefabEntry entry,
            @Nullable BuilderTool tool,
            @Nullable ItemStack stack,
            @Nonnull Random random) {
        BlockSelection selection = HyzaliaPastePrefabLoader.loadSelection(entry).cloneSelection();
        applyToolRandomTransforms(selection, tool, stack, random);
        return selection;
    }

    static void applyToolRandomTransforms(
            @Nonnull BlockSelection selection,
            @Nullable BuilderTool tool,
            @Nullable ItemStack stack,
            @Nonnull Random random) {
        if (tool == null || stack == null) {
            return;
        }
        String randomize = HyzaliaPasteToolArgs.randomizeRotation(tool, stack);
        if ("No".equalsIgnoreCase(randomize)) {
            return;
        }

        RotateBlockMode rotateBlockMode = HyzaliaPasteToolArgs.rotateBlockMode(tool, stack);
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

        if (deltaX != 0) {
            selection.rotate(Axis.X, deltaX, rotateBlockMode);
        }
        if (deltaY != 0) {
            selection.rotate(Axis.Y, deltaY, rotateBlockMode);
        }
        if (deltaZ != 0) {
            selection.rotate(Axis.Z, deltaZ, rotateBlockMode);
        }

        if (HyzaliaPasteToolArgs.randomFlip(tool, stack)) {
            selection.flip(Axis.X);
            selection.flip(Axis.Y);
            selection.flip(Axis.Z);
        }
    }

    static void loadPendingIntoClipboard(
            @Nonnull BuilderToolsPlugin.BuilderState builderState,
            @Nonnull WeightedPrefabEntry entry,
            @Nullable BuilderTool tool,
            @Nullable ItemStack stack,
            @Nonnull Random random) {
        setClipboardWithoutHistory(builderState, prepareSelection(entry, tool, stack, random));
    }

    static void ensurePendingClipboardLoaded(
            @Nonnull BuilderToolsPlugin.BuilderState builderState,
            @Nonnull MultiPrefabPasteState state,
            @Nullable BuilderTool tool,
            @Nullable ItemStack stack,
            @Nonnull Random random) {
        if (builderState.getSelection() != null) {
            return;
        }
        WeightedPrefabEntry pending = state.pendingPasteEntry();
        if (pending == null) {
            return;
        }
        loadPendingIntoClipboard(builderState, pending, tool, stack, random);
    }

    private static int pickRightAngle(@Nonnull Random random) {
        return random.nextInt(4) * 90;
    }
}
