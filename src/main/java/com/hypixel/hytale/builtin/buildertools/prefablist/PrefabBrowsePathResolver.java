package com.hypixel.hytale.builtin.buildertools.prefablist;

import com.hypixel.hytale.server.core.prefab.PrefabStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;

/** Résolution de chemins prefab (même logique que {@link PrefabPage}). */
public final class PrefabBrowsePathResolver {

    private static final String PREFAB_EXTENSION = ".prefab.json";
    private static final String PREFAB_ICON_SUFFIX = ".prefab.icon";
    private static final String SERVER_PREFABS_PREFIX = "Server/Prefabs/";

    private PrefabBrowsePathResolver() {
    }

    /** Chemin prefab résolu : fichier disque + clé virtuelle navigateur. */
    public record ResolvedPrefab(@Nonnull Path filePath, @Nonnull String virtualPath) {
    }

    @Nonnull
    public static String buildVirtualPath(@Nonnull Path assetsCurrentDir, @Nonnull String preview) {
        if (preview.contains("/")) {
            return preview.replace('\\', '/');
        }
        String dir = assetsCurrentDir.toString().replace('\\', '/');
        if (dir.isEmpty()) {
            return preview;
        }
        return dir + "/" + preview;
    }

    /** Même logique que {@code PrefabPage#resolvePreviewPath}. */
    @Nullable
    public static ResolvedPrefab resolvePreviewPath(
            @Nonnull Path assetsCurrentDir,
            @Nonnull AssetPrefabFileProvider provider,
            @Nonnull String preview) {
        String virtual = buildVirtualPath(assetsCurrentDir, preview);
        Path resolved = provider.resolveVirtualPath(virtual);
        if (isPrefabFile(resolved)) {
            return new ResolvedPrefab(resolved, virtual);
        }
        Path fallback = resolveVirtual(provider, virtual);
        if (fallback != null) {
            return new ResolvedPrefab(fallback, virtual);
        }
        return null;
    }

    @Nullable
    public static Path resolvePreview(
            @Nonnull Path assetsCurrentDir,
            @Nonnull AssetPrefabFileProvider provider,
            @Nonnull String preview) {
        ResolvedPrefab resolved = resolvePreviewPath(assetsCurrentDir, provider, preview);
        return resolved != null ? resolved.filePath() : null;
    }

    @Nullable
    public static Path resolveVirtual(
            @Nonnull AssetPrefabFileProvider provider,
            @Nonnull String virtualPath) {
        String normalized = normalizeVirtual(virtualPath);
        Path resolved = provider.resolveVirtualPath(normalized);
        if (isPrefabFile(resolved)) {
            return resolved;
        }
        if (normalized.endsWith(PREFAB_ICON_SUFFIX)) {
            resolved = provider.resolveVirtualPath(toPrefabJsonVirtual(normalized));
            if (isPrefabFile(resolved)) {
                return resolved;
            }
        }
        if (!normalized.contains("/")) {
            resolved = provider.resolveVirtualPath("HytaleAssets/" + normalized);
            if (isPrefabFile(resolved)) {
                return resolved;
            }
            if (normalized.endsWith(PREFAB_ICON_SUFFIX)) {
                resolved = provider.resolveVirtualPath("HytaleAssets/" + toPrefabJsonVirtual(normalized));
                if (isPrefabFile(resolved)) {
                    return resolved;
                }
            }
        }
        return null;
    }

    /**
     * Résout une clé persistée (virtuelle navigateur, chemin absolu, ou ancien format {@code Server/Prefabs/...}).
     */
    @Nullable
    public static Path resolveStoredPath(
            @Nonnull AssetPrefabFileProvider provider,
            @Nonnull String storedPath) {
        String normalized = storedPath.replace('\\', '/');

        Path fromVirtual = resolveVirtual(provider, normalized);
        if (fromVirtual != null) {
            return fromVirtual.normalize();
        }

        Path direct = Path.of(normalized);
        if (isPrefabFile(direct)) {
            return direct.normalize();
        }
        if (isPrefabFile(direct.toAbsolutePath().normalize())) {
            return direct.toAbsolutePath().normalize();
        }

        Path legacy = resolveLegacyServerPrefabsPath(provider, normalized);
        if (legacy != null) {
            return legacy.normalize();
        }

        if (normalized.endsWith(PREFAB_ICON_SUFFIX)) {
            Path iconPath = Path.of(normalized);
            if (Files.isRegularFile(iconPath)) {
                Path sibling = iconPath.resolveSibling(toPrefabJsonFileName(iconPath.getFileName().toString()));
                if (isPrefabFile(sibling)) {
                    return sibling.normalize();
                }
            }
        }
        return null;
    }

    /** Clé à persister : chemin virtuel navigateur (ex. {@code HytaleAssets/Trees/.../foo.prefab.json}). */
    @Nonnull
    public static String toStoredVirtualPath(@Nonnull String virtualPath) {
        return normalizeVirtual(virtualPath);
    }

    @Nonnull
    public static String toDisplayName(@Nonnull String virtualPath) {
        String fileName = virtualPath;
        int slash = virtualPath.lastIndexOf('/');
        if (slash >= 0 && slash < virtualPath.length() - 1) {
            fileName = virtualPath.substring(slash + 1);
        }
        return removePrefabExtension(fileName);
    }

    @Nonnull
    public static String toDisplayName(@Nonnull Path prefabFile) {
        Path fileName = prefabFile.getFileName();
        if (fileName == null) {
            return prefabFile.toString();
        }
        return removePrefabExtension(fileName.toString());
    }

    @Nonnull
    public static String removePrefabExtension(@Nonnull String fileName) {
        if (fileName.endsWith(PREFAB_EXTENSION)) {
            return fileName.substring(0, fileName.length() - PREFAB_EXTENSION.length());
        }
        if (fileName.endsWith(PREFAB_ICON_SUFFIX)) {
            return fileName.substring(0, fileName.length() - PREFAB_ICON_SUFFIX.length());
        }
        return fileName;
    }

    @Nonnull
    private static String normalizeVirtual(@Nonnull String virtualPath) {
        String normalized = virtualPath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    @Nullable
    private static Path resolveLegacyServerPrefabsPath(
            @Nonnull AssetPrefabFileProvider provider,
            @Nonnull String normalized) {
        String trimmed = normalizeVirtual(normalized);
        if (trimmed.startsWith(SERVER_PREFABS_PREFIX)) {
            trimmed = trimmed.substring(SERVER_PREFABS_PREFIX.length());
        }
        if (!trimmed.endsWith(PREFAB_EXTENSION) && !trimmed.contains(".")) {
            trimmed = trimmed + PREFAB_EXTENSION;
        }

        Path resolved = resolveVirtual(provider, "HytaleAssets/" + trimmed);
        if (resolved != null) {
            return resolved;
        }

        for (PrefabStore.AssetPackPrefabPath pack : PrefabStore.get().getAllBrowsablePrefabPaths()) {
            Path candidate = pack.prefabsPath().resolve(trimmed);
            if (isPrefabFile(candidate)) {
                return candidate;
            }
            resolved = resolveVirtual(provider, pack.getDisplayName() + "/" + trimmed);
            if (resolved != null) {
                return resolved;
            }
            resolved = resolveVirtual(provider, pack.getPackName() + "/" + trimmed);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    @Nonnull
    private static String toPrefabJsonVirtual(@Nonnull String virtualPath) {
        if (virtualPath.endsWith(PREFAB_ICON_SUFFIX)) {
            return virtualPath.substring(0, virtualPath.length() - PREFAB_ICON_SUFFIX.length()) + PREFAB_EXTENSION;
        }
        return virtualPath;
    }

    @Nonnull
    private static String toPrefabJsonFileName(@Nonnull String fileName) {
        if (fileName.endsWith(PREFAB_ICON_SUFFIX)) {
            return fileName.substring(0, fileName.length() - PREFAB_ICON_SUFFIX.length()) + PREFAB_EXTENSION;
        }
        return fileName;
    }

    private static boolean isPrefabFile(@Nullable Path path) {
        if (path == null) {
            return false;
        }
        try {
            if (!Files.isRegularFile(path)) {
                return false;
            }
            String name = path.getFileName() != null ? path.getFileName().toString() : path.toString();
            return name.endsWith(PREFAB_EXTENSION);
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
