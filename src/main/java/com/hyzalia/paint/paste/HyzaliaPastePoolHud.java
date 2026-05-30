package com.hyzalia.paint.paste;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.modules.entity.player.ChunkTracker;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;

/** HUD affichant la liste pondérée de prefabs lorsque l'outil Hyzalia Paste est actif. */
public final class HyzaliaPastePoolHud extends CustomUIHud {

    public static final String HUD_KEY = "hyzalia-paste-pool";

    private static final int MAX_ROWS = 8;

    private static final String UI_PATH = "HyzaliaPastePoolLegend.ui";

    public HyzaliaPastePoolHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, HUD_KEY);
    }

    public static void showOrRefresh(
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store) {
        if (!HyzaliaPasteToolItems.isHoldingHyzaliaPasteTool(ref, store)) {
            hideIfPresent(player, playerRef);
            return;
        }
        if (!isClientReadyForHud(ref, store)) {
            return;
        }

        HudManager hudManager = player.getHudManager();
        HyzaliaPastePoolHud hud = (HyzaliaPastePoolHud) hudManager.getCustomHud(HUD_KEY);
        if (hud == null) {
            hud = new HyzaliaPastePoolHud(playerRef);
            hudManager.addCustomHud(playerRef, hud);
        } else {
            hud.refresh(ref, store);
        }
    }

    public static void refreshIfActive(
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store) {
        if (!HyzaliaPasteToolItems.isHoldingHyzaliaPasteTool(ref, store)) {
            return;
        }
        HudManager hudManager = player.getHudManager();
        HyzaliaPastePoolHud hud = (HyzaliaPastePoolHud) hudManager.getCustomHud(HUD_KEY);
        if (hud != null) {
            hud.refresh(ref, store);
        } else {
            showOrRefresh(player, playerRef, ref, store);
        }
    }

    public static void hideIfPresent(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        HudManager hudManager = player.getHudManager();
        if (hudManager.getCustomHud(HUD_KEY) != null) {
            hudManager.removeCustomHud(playerRef, HUD_KEY);
        }
    }

    private static boolean isClientReadyForHud(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store) {
        ChunkTracker tracker = store.getComponent(ref, ChunkTracker.getComponentType());
        return tracker == null || tracker.isReadyForChunks();
    }

    private void refresh(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder builder = new UICommandBuilder();
        applyPoolContent(builder, MultiPrefabPasteState.ensure(ref, store));
        update(false, builder);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append(UI_PATH);
        applyPoolContent(builder, new MultiPrefabPasteState());
    }

    private static void applyPoolContent(
            @Nonnull UICommandBuilder builder,
            @Nonnull MultiPrefabPasteState state) {
        List<WeightedPrefabEntry> entries = state.entriesView();
        builder.set(
                "#PoolTitle.TextSpans",
                Message.translation("server.hyzalia.paint.paste.poolHudTitle"));

        if (entries.isEmpty()) {
            builder.set("#PoolEmpty.Visible", true);
            builder.set(
                    "#PoolEmpty.TextSpans",
                    Message.translation("server.hyzalia.paint.paste.emptyList"));
            for (int i = 0; i < MAX_ROWS; i++) {
                builder.set("#PoolRow" + i + ".Visible", false);
            }
            return;
        }

        builder.set("#PoolEmpty.Visible", false);
        int totalWeight = state.totalWeight();
        int rowCount = Math.min(entries.size(), MAX_ROWS);
        for (int i = 0; i < rowCount; i++) {
            WeightedPrefabEntry entry = entries.get(i);
            int percent = totalWeight > 0 ? Math.round(entry.weight() * 100f / totalWeight) : 0;
            builder.set("#PoolRow" + i + ".Visible", true);
            builder.set(
                    "#PoolRow" + i + ".TextSpans",
                    Message.translation("server.hyzalia.paint.paste.poolHudRow")
                            .param("index", i + 1)
                            .param("name", entry.displayName())
                            .param("weight", entry.weight())
                            .param("percent", percent));
        }
        for (int i = rowCount; i < MAX_ROWS; i++) {
            builder.set("#PoolRow" + i + ".Visible", false);
        }
        if (entries.size() > MAX_ROWS) {
            builder.set("#PoolMore.Visible", true);
            builder.set(
                    "#PoolMore.TextSpans",
                    Message.translation("server.hyzalia.paint.paste.poolHudMore")
                            .param("count", entries.size() - MAX_ROWS));
        } else {
            builder.set("#PoolMore.Visible", false);
        }
    }
}
