package com.hyzalia.paint.paste;

import com.hypixel.hytale.builtin.buildertools.prefablist.AssetPrefabFileProvider;
import com.hypixel.hytale.builtin.buildertools.prefablist.PrefabBrowsePathResolver;
import com.hypixel.hytale.server.core.prefab.PrefabLoadException;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.PrefabWeights;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/** Sélection pondérée et chargement prefab depuis le store. */
public final class HyzaliaPastePrefabLoader {

    private static final AssetPrefabFileProvider PATH_PROVIDER = new AssetPrefabFileProvider();

    private HyzaliaPastePrefabLoader() {
    }

    @Nullable
    public static WeightedPrefabEntry pickWeighted(@Nonnull List<WeightedPrefabEntry> entries, @Nonnull Random random) {
        if (entries.isEmpty()) {
            return null;
        }
        if (entries.size() == 1) {
            return entries.getFirst();
        }
        PrefabWeights weights = new PrefabWeights();
        for (WeightedPrefabEntry entry : entries) {
            weights.setWeight(entry.weightKey(), entry.weight());
        }
        WeightedPrefabEntry[] array = entries.toArray(WeightedPrefabEntry[]::new);
        Function<WeightedPrefabEntry, String> keyFn = WeightedPrefabEntry::weightKey;
        return weights.get(array, keyFn, random);
    }

    @Nonnull
    public static BlockSelection loadSelection(@Nonnull WeightedPrefabEntry entry) {
        Path path = resolvePrefabPath(entry.prefabPath());
        if (path == null) {
            throw new PrefabLoadException(
                    PrefabLoadException.Type.NOT_FOUND,
                    "Prefab not found: " + entry.prefabPath());
        }
        BlockSelection selection = PrefabStore.get().getPrefab(path);
        if (selection == null) {
            throw new PrefabLoadException(
                    PrefabLoadException.Type.NOT_FOUND,
                    "Prefab not found: " + path);
        }
        return selection;
    }

    @Nullable
    public static Path resolvePrefabPath(@Nonnull String storedPath) {
        return PrefabBrowsePathResolver.resolveStoredPath(PATH_PROVIDER, storedPath);
    }
}
