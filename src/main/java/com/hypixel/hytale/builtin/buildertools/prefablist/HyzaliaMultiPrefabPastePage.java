package com.hypixel.hytale.builtin.buildertools.prefablist;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.ui.browser.FileBrowserEventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hyzalia.paint.paste.HyzaliaPasteToolConstants;
import com.hyzalia.paint.paste.HyzaliaPasteToolService;
import com.hyzalia.paint.paste.MultiPrefabListPage;
import com.hyzalia.paint.paste.MultiPrefabPasteState;
import com.hyzalia.paint.paste.WeightedPrefabEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Navigateur prefab : le bouton « Add Prefab » ajoute à la pool pondérée (pas au clipboard vanilla).
 */
public final class HyzaliaMultiPrefabPastePage extends PrefabPage {

    private final AssetPrefabFileProvider pathProvider = new AssetPrefabFileProvider();
    private final boolean returnToManageAfterAdd;
    private Path assetsCurrentDir = initialAssetsCurrentDir();

    @Nullable
    private PrefabBrowsePathResolver.ResolvedPrefab selectedPrefab;

    public HyzaliaMultiPrefabPastePage(
            @Nonnull PlayerRef playerRef,
            @Nonnull BuilderToolsPlugin.BuilderState builderState) {
        this(playerRef, builderState, false);
    }

    public HyzaliaMultiPrefabPastePage(
            @Nonnull PlayerRef playerRef,
            @Nonnull BuilderToolsPlugin.BuilderState builderState,
            boolean returnToManageAfterAdd) {
        super(playerRef, builderState);
        this.returnToManageAfterAdd = returnToManageAfterAdd;
    }

    @Override
    public void build(
            Ref<EntityStore> ref,
            UICommandBuilder commandBuilder,
            UIEventBuilder eventBuilder,
            Store<EntityStore> store) {
        super.build(ref, commandBuilder, eventBuilder, store);
        commandBuilder.set(
                "#CurrentPath.Text",
                Message.translation("server.hyzalia.paint.paste.addBrowserTitle"));
        commandBuilder.set(
                "#LoadButton.TextSpans",
                Message.translation("server.hyzalia.paint.paste.addPrefab"));
    }

    @Override
    public void handleDataEvent(
            Ref<EntityStore> ref,
            Store<EntityStore> store,
            FileBrowserEventData pageData) {
        if (pageData.getSearchQuery() != null) {
            super.handleDataEvent(ref, store, pageData);
            return;
        }

        if (pageData.isBrowseRequested()) {
            PrefabBrowsePathResolver.ResolvedPrefab resolved = resolveLoadSelection(pageData);
            if (resolved != null) {
                addSelectedPrefab(ref, store, resolved);
            } else {
                playerRef.sendMessage(Message.translation("server.hyzalia.paint.paste.noSelection"));
            }
            return;
        }

        if (pageData.getPreview() != null && !pageData.getPreview().isBlank()) {
            PrefabBrowsePathResolver.ResolvedPrefab resolved = resolvePreviewPath(pageData.getPreview());
            if (resolved != null) {
                selectedPrefab = resolved;
            }
            super.handleDataEvent(ref, store, pageData);
            return;
        }

        String navigationTarget = pageData.getSearchResult() != null && !pageData.getSearchResult().isBlank()
                ? pageData.getSearchResult()
                : pageData.getFile();
        if (navigationTarget != null && !navigationTarget.isBlank()) {
            trackAssetsNavigation(navigationTarget, pageData.getSearchResult() != null);
        }

        super.handleDataEvent(ref, store, pageData);
    }

    @Nullable
    private PrefabBrowsePathResolver.ResolvedPrefab resolveLoadSelection(@Nonnull FileBrowserEventData pageData) {
        if (pageData.getPreview() != null && !pageData.getPreview().isBlank()) {
            PrefabBrowsePathResolver.ResolvedPrefab fromEvent = resolvePreviewPath(pageData.getPreview());
            if (fromEvent != null) {
                return fromEvent;
            }
        }
        return selectedPrefab;
    }

    @Nullable
    private PrefabBrowsePathResolver.ResolvedPrefab resolvePreviewPath(@Nonnull String preview) {
        return PrefabBrowsePathResolver.resolvePreviewPath(assetsCurrentDir, pathProvider, preview);
    }

    private void trackAssetsNavigation(@Nonnull String target, boolean fromSearchResult) {
        if ("~".equals(target)) {
            assetsCurrentDir = initialAssetsCurrentDir();
            return;
        }
        if ("..".equals(target)) {
            if (assetsCurrentDir.getNameCount() > 1) {
                Path parent = assetsCurrentDir.getParent();
                if (parent != null) {
                    assetsCurrentDir = parent;
                }
            } else if (assetsCurrentDir.getNameCount() == 1) {
                assetsCurrentDir = Path.of("");
            }
            return;
        }

        String virtual = fromSearchResult
                ? target
                : PrefabBrowsePathResolver.buildVirtualPath(assetsCurrentDir, target);
        Path resolved = pathProvider.resolveVirtualPath(virtual.replace('\\', '/'));
        if (resolved == null) {
            return;
        }
        try {
            if (Files.isDirectory(resolved)) {
                assetsCurrentDir = Paths.get(virtual.replace('\\', '/'));
            }
        } catch (RuntimeException ignored) {
            // no-op
        }
    }

    private static Path initialAssetsCurrentDir() {
        if (PrefabStore.get().getAllBrowsablePrefabPaths().size() > 1) {
            return Path.of("");
        }
        return Path.of("HytaleAssets");
    }

    private void addSelectedPrefab(
            Ref<EntityStore> ref,
            Store<EntityStore> store,
            @Nonnull PrefabBrowsePathResolver.ResolvedPrefab resolved) {
        Player player = Objects.requireNonNull(
                store.getComponent(ref, Player.getComponentType()),
                "Player component");

        String prefabPath = PrefabBrowsePathResolver.toStoredVirtualPath(resolved.virtualPath());
        String displayName = PrefabBrowsePathResolver.toDisplayName(resolved.virtualPath());

        MultiPrefabPasteState state = MultiPrefabPasteState.ensure(ref, store);
        state.addEntry(new WeightedPrefabEntry(
                prefabPath,
                displayName,
                HyzaliaPasteToolConstants.DEFAULT_WEIGHT));

        playerRef.sendMessage(Message.translation("server.hyzalia.paint.paste.added")
                .param("name", displayName)
                .param("weight", HyzaliaPasteToolConstants.DEFAULT_WEIGHT));

        HyzaliaPasteToolService.syncPreviewClipboard(player, playerRef, ref, state, store);

        selectedPrefab = null;
        PageManager pageManager = player.getPageManager();
        if (returnToManageAfterAdd) {
            pageManager.openCustomPage(ref, store, new MultiPrefabListPage(playerRef));
        } else {
            pageManager.setPage(ref, store, com.hypixel.hytale.protocol.packets.interface_.Page.None);
        }
    }
}
