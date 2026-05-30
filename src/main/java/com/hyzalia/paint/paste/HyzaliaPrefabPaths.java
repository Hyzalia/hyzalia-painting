package com.hyzalia.paint.paste;

import com.hypixel.hytale.builtin.buildertools.prefablist.AssetPrefabFileProvider;
import com.hypixel.hytale.builtin.buildertools.prefablist.PrefabBrowsePathResolver;
import com.hypixel.hytale.server.core.ui.browser.FileBrowserEventData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;

/** Résolution de chemins prefab via l'API publique BuilderTools (sans réflexion). */
public final class HyzaliaPrefabPaths {

    private static final AssetPrefabFileProvider PROVIDER = new AssetPrefabFileProvider();
    private static final Path DEFAULT_ASSETS_DIR = Path.of("");

    private HyzaliaPrefabPaths() {
    }

    @Nullable
    public static Path resolveBrowseSelection(@Nonnull FileBrowserEventData pageData) {
        String preview = pageData.getPreview();
        if (preview != null && !preview.isBlank()) {
            return resolvePreview(preview);
        }
        String file = pageData.getFile();
        if (file != null && !file.isBlank()) {
            return resolvePreview(file);
        }
        return null;
    }

    @Nullable
    public static Path resolvePreview(@Nonnull String preview) {
        return PrefabBrowsePathResolver.resolvePreview(DEFAULT_ASSETS_DIR, PROVIDER, preview);
    }
}
