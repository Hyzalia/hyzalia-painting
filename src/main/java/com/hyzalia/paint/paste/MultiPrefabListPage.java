package com.hyzalia.paint.paste;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.builtin.buildertools.prefablist.PrefabPreviewSender;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

/**
 * Gestion du pool pondéré.
 * Utilise {@code HyzaliaPasteManageShell.ui} (overlay plein écran + preview {@code #PrefabPreview}).
 */
public final class MultiPrefabListPage extends InteractiveCustomUIPage<MultiPrefabListPage.PageData> {

    private static final int MAX_VISIBLE_ROWS = 12;
    /** Shell plein écran (sans marge {@code Top: 130} du {@code Pages/PrefabListPage.ui} vanilla). */
    private static final String SHELL_UI = "HyzaliaPasteManageShell.ui";
    private static final String ROW_UI = "HyzaliaPastePoolRow.ui";

    public MultiPrefabListPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
    }

    @Override
    public void build(
            Ref<EntityStore> ref,
            UICommandBuilder commandBuilder,
            UIEventBuilder eventBuilder,
            Store<EntityStore> store) {
        commandBuilder.append(SHELL_UI);
        populate(ref, store, commandBuilder, eventBuilder);
    }

    @Override
    public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) {
        PrefabPreviewSender.clearPreview(playerRef);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            HyzaliaPasteToolService.syncPreviewClipboard(
                    player,
                    playerRef,
                    ref,
                    MultiPrefabPasteState.ensure(ref, store),
                    store);
        }
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, PageData pageData) {
        Player player = Objects.requireNonNull(
                store.getComponent(ref, Player.getComponentType()),
                "Player component");
        MultiPrefabPasteState state = MultiPrefabPasteState.ensure(ref, store);

        if (pageData.action == PageData.Action.AddPrefab) {
            BuilderToolsPlugin.BuilderState builderState = BuilderToolsPlugin.getState(player, playerRef);
            player.getPageManager().openCustomPage(
                    ref,
                    store,
                    new com.hypixel.hytale.builtin.buildertools.prefablist.HyzaliaMultiPrefabPastePage(
                            playerRef,
                            builderState,
                            true));
            return;
        }

        if (pageData.rowIndex != null && pageData.action == PageData.Action.Select) {
            int index = parseIndex(pageData.rowIndex);
            if (index < 0 || index >= state.size()) {
                sendUpdate(null, null, false);
                return;
            }
            if (index == state.selectedIndex()) {
                sendUpdate(null, null, false);
                return;
            }
            state.setSelectedIndex(index);
            syncInGamePreviewIfHeld(player, playerRef, ref, state, store);
            sendPreviewForState(state);
            refresh(ref, store);
            return;
        }

        if (pageData.rowIndex != null && pageData.action == PageData.Action.Remove) {
            int index = parseIndex(pageData.rowIndex);
            if (index < 0 || index >= state.size()) {
                playerRef.sendMessage(Message.translation("server.hyzalia.paint.paste.invalidIndex"));
                sendUpdate(null, null, false);
                return;
            }
            WeightedPrefabEntry removed = state.entriesView().get(index);
            state.removeAt(index);
            playerRef.sendMessage(Message.translation("server.hyzalia.paint.paste.removed")
                    .param("name", removed.displayName()));
            HyzaliaPasteToolService.syncPreviewClipboard(player, playerRef, ref, state, store);
            refresh(ref, store);
            return;
        }

        if (pageData.rowIndex != null && pageData.action == PageData.Action.SetWeight) {
            int index = parseIndex(pageData.rowIndex);
            int weight = resolveWeight(pageData);
            if (weight < 0) {
                sendUpdate(null, null, false);
                return;
            }
            if (index < 0 || index >= state.size()) {
                sendUpdate(null, null, false);
                return;
            }
            if (weight < 1 || weight > 1000) {
                sendUpdate(null, null, false);
                return;
            }
            WeightedPrefabEntry entry = state.entriesView().get(index);
            if (entry.weight() == weight) {
                sendUpdate(null, null, false);
                return;
            }
            state.setWeightAt(index, weight);
            syncInGamePreviewIfHeld(player, playerRef, ref, state, store);
            refreshPercentages(ref, store);
            return;
        }

        sendUpdate(null, null, false);
    }

    private void refresh(Ref<EntityStore> ref, Store<EntityStore> store) {
        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        populate(ref, store, commandBuilder, eventBuilder);
        sendUpdate(commandBuilder, eventBuilder, false);
    }

    private void refreshPercentages(Ref<EntityStore> ref, Store<EntityStore> store) {
        MultiPrefabPasteState state = MultiPrefabPasteState.ensure(ref, store);
        List<WeightedPrefabEntry> entries = state.entriesView();
        if (entries.isEmpty()) {
            refresh(ref, store);
            return;
        }
        UICommandBuilder commandBuilder = new UICommandBuilder();
        int totalWeight = state.totalWeight();
        int rowCount = Math.min(entries.size(), MAX_VISIBLE_ROWS);
        for (int i = 0; i < rowCount; i++) {
            int percent = totalWeight > 0 ? Math.round(entries.get(i).weight() * 100f / totalWeight) : 0;
            commandBuilder.set("#FileList[" + i + "] #Percent.Text", percent + "%");
        }
        sendUpdate(commandBuilder, null, false);
    }

    private void populate(
            Ref<EntityStore> ref,
            Store<EntityStore> store,
            UICommandBuilder commandBuilder,
            UIEventBuilder eventBuilder) {
        MultiPrefabPasteState state = MultiPrefabPasteState.ensure(ref, store);
        List<WeightedPrefabEntry> entries = state.entriesView();

        commandBuilder.set(
                "#CurrentPath.TextSpans",
                Message.translation("server.hyzalia.paint.paste.manageTitle"));
        commandBuilder.set("#SearchInput.Visible", false);
        commandBuilder.set("#HomeButton.Visible", false);
        commandBuilder.set("#LoadButton.Visible", true);
        commandBuilder.set(
                "#LoadButton.TextSpans",
                Message.translation("server.hyzalia.paint.paste.addPrefab"));

        commandBuilder.clear("#FileList");

        if (entries.isEmpty()) {
            PrefabPreviewSender.clearPreview(playerRef);
        } else {
            int totalWeight = state.totalWeight();
            int rowCount = Math.min(entries.size(), MAX_VISIBLE_ROWS);
            int selected = state.selectedIndex();
            for (int i = 0; i < rowCount; i++) {
                bindRow(commandBuilder, eventBuilder, entries.get(i), i, totalWeight, i == selected);
            }
            schedulePreview(state);
        }

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#LoadButton",
                EventData.of("Action", PageData.Action.AddPrefab.name()));
    }

    private void sendPreviewForState(@Nonnull MultiPrefabPasteState state) {
        schedulePreview(state);
    }

    private void schedulePreview(@Nonnull MultiPrefabPasteState state) {
        WeightedPrefabEntry selected = state.selectedEntry();
        if (selected == null) {
            PrefabPreviewSender.clearPreview(playerRef);
            return;
        }
        try {
            BlockSelection selection = HyzaliaPastePrefabLoader.loadSelection(selected);
            PrefabPreviewSender.sendPreview(playerRef, selection);
        } catch (RuntimeException ex) {
            PrefabPreviewSender.clearPreview(playerRef);
        }
    }

    private static void syncInGamePreviewIfHeld(
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull MultiPrefabPasteState state,
            @Nonnull Store<EntityStore> store) {
        if (HyzaliaPasteHeldTool.getHeld(ref, store).isPresent()) {
            HyzaliaPasteToolService.syncPreviewClipboard(player, playerRef, ref, state, store);
        }
    }

    private static void bindRow(
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull WeightedPrefabEntry entry,
            int index,
            int totalWeight,
            boolean selected) {
        String row = "#FileList[" + index + "]";
        int percent = totalWeight > 0 ? Math.round(entry.weight() * 100f / totalWeight) : 0;

        commandBuilder.append("#FileList", ROW_UI);
        commandBuilder.set(row + " #SelectMark.Text", selected ? "*" : "-");
        commandBuilder.set(row + " #SelectRow.Text", (index + 1) + ". " + entry.displayName());
        commandBuilder.set(row + " #Percent.Text", percent + "%");
        commandBuilder.set(row + " #Weight.Value", entry.weight());

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                row + " #SelectRow",
                EventData.of("Action", PageData.Action.Select.name())
                        .append("RowIndex", String.valueOf(index)));
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                row + " #Weight",
                EventData.of("Action", PageData.Action.SetWeight.name())
                        .append("RowIndex", String.valueOf(index))
                        .append("@Weight", row + " #Weight.Value"),
                false);
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                row + " #Remove",
                EventData.of("Action", PageData.Action.Remove.name())
                        .append("RowIndex", String.valueOf(index)));
    }

    private static int resolveWeight(@Nonnull PageData pageData) {
        if (pageData.weightFloat != null) {
            return Math.round(pageData.weightFloat);
        }
        if (pageData.weight != null && !pageData.weight.isBlank()) {
            return parseWeight(pageData.weight);
        }
        return -1;
    }

    private static int parseIndex(@Nonnull String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static int parseWeight(@Nonnull String raw) {
        try {
            String normalized = raw.trim().replace(',', '.');
            if (normalized.contains(".")) {
                return (int) Math.round(Double.parseDouble(normalized));
            }
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public static final class PageData {

        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (data, value) -> {
                    if (value != null) {
                        data.action = Action.valueOf(value);
                    }
                }, data -> data.action != null ? data.action.name() : null)
                .add()
                .append(new KeyedCodec<>("RowIndex", Codec.STRING), (data, value) -> data.rowIndex = value, data -> data.rowIndex)
                .add()
                .append(new KeyedCodec<>("Weight", Codec.STRING), (data, value) -> data.weight = value, data -> data.weight)
                .add()
                .append(new KeyedCodec<>("@Weight", Codec.FLOAT), (data, value) -> {
                    if (value != null) {
                        data.weightFloat = value;
                    }
                }, data -> data.weightFloat)
                .add()
                .build();

        public enum Action {
            Select,
            Remove,
            SetWeight,
            AddPrefab
        }

        public Action action;
        public String rowIndex;
        public String weight;
        public Float weightFloat;
    }
}
