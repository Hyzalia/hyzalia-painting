package com.hyzalia.paint.paste;

import com.hypixel.hytale.server.core.asset.type.buildertool.config.BuilderTool;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.prefab.selection.standard.RotateBlockMode;

import javax.annotation.Nonnull;
import java.util.Map;

/** Lecture des args BuilderTool sur l'item paste Hyzalia. */
public final class HyzaliaPasteToolArgs {

    private HyzaliaPasteToolArgs() {
    }

    @Nonnull
    public static RotateBlockMode rotateBlockMode(@Nonnull BuilderTool tool, @Nonnull ItemStack stack) {
        Map<String, Object> args = toolArgs(tool, stack);
        if (args == null) {
            return RotateBlockMode.NON_UNIFORM;
        }
        Object value = args.getOrDefault("RotateBlock", "NonUniform");
        return RotateBlockMode.fromString(String.valueOf(value));
    }

    public static boolean pasteAir(@Nonnull BuilderTool tool, @Nonnull ItemStack stack) {
        return boolArg(tool, stack, "PasteAir", false);
    }

    @Nonnull
    public static String randomizeRotation(@Nonnull BuilderTool tool, @Nonnull ItemStack stack) {
        Map<String, Object> args = toolArgs(tool, stack);
        if (args == null) {
            return "No";
        }
        Object value = args.getOrDefault("RandomizeRotation", "No");
        return String.valueOf(value);
    }

    public static boolean randomFlip(@Nonnull BuilderTool tool, @Nonnull ItemStack stack) {
        return boolArg(tool, stack, "RandomFlip", false);
    }

    private static boolean boolArg(
            @Nonnull BuilderTool tool,
            @Nonnull ItemStack stack,
            @Nonnull String key,
            boolean defaultValue) {
        Map<String, Object> args = toolArgs(tool, stack);
        if (args == null) {
            return defaultValue;
        }
        Object value = args.getOrDefault(key, defaultValue);
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static Map<String, Object> toolArgs(@Nonnull BuilderTool tool, @Nonnull ItemStack stack) {
        BuilderTool.ArgData data = tool.getItemArgData(stack);
        if (data == null || data.tool() == null) {
            return null;
        }
        return data.tool();
    }
}
