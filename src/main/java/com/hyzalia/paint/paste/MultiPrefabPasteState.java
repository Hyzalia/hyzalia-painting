package com.hyzalia.paint.paste;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Liste de prefabs pondérées associée au joueur (composant ECS). */
public final class MultiPrefabPasteState implements Component<EntityStore> {

    public static final String COMPONENT_ID = "HyzaliaWeightedPastePool";

    public static final BuilderCodec<MultiPrefabPasteState> CODEC = BuilderCodec.builder(
                    MultiPrefabPasteState.class,
                    MultiPrefabPasteState::new)
            .append(
                    new KeyedCodec<>(
                            "Entries",
                            ArrayCodec.ofBuilderCodec(WeightedPrefabEntry.CODEC, WeightedPrefabEntry[]::new)),
                    (state, entries) -> {
                        state.entries.clear();
                        if (entries != null) {
                            Collections.addAll(state.entries, entries);
                        }
                    },
                    state -> state.entries.toArray(WeightedPrefabEntry[]::new))
            .add()
            .append(new KeyedCodec<>("SelectedIndex", Codec.INTEGER), (state, value) -> {
                state.selectedIndex = value != null ? value : -1;
            }, state -> state.selectedIndex)
            .add()
            .build();

    private static ComponentType<EntityStore, MultiPrefabPasteState> componentType;

    private final List<WeightedPrefabEntry> entries = new ArrayList<>();
    private int selectedIndex = -1;

    public static void registerComponentType(@Nonnull ComponentType<EntityStore, MultiPrefabPasteState> type) {
        componentType = type;
    }

    @Nullable
    public static ComponentType<EntityStore, MultiPrefabPasteState> getComponentType() {
        return componentType;
    }

    @Nonnull
    public static MultiPrefabPasteState ensure(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store) {
        ComponentType<EntityStore, MultiPrefabPasteState> type = Objects.requireNonNull(
                componentType,
                "MultiPrefabPasteState component type not registered");
        MultiPrefabPasteState state = store.getComponent(ref, type);
        if (state == null) {
            state = new MultiPrefabPasteState();
            store.addComponent(ref, type, state);
        }
        return state;
    }

    @Nonnull
    public List<WeightedPrefabEntry> entriesView() {
        return Collections.unmodifiableList(entries);
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void addEntry(@Nonnull WeightedPrefabEntry entry) {
        entries.add(Objects.requireNonNull(entry, "entry"));
        selectedIndex = entries.size() - 1;
    }

    public boolean removeAt(int index) {
        if (index < 0 || index >= entries.size()) {
            return false;
        }
        entries.remove(index);
        if (entries.isEmpty()) {
            selectedIndex = -1;
        } else if (selectedIndex >= entries.size()) {
            selectedIndex = entries.size() - 1;
        } else if (selectedIndex == index) {
            selectedIndex = Math.min(index, entries.size() - 1);
        }
        return true;
    }

    @Nonnull
    public Optional<WeightedPrefabEntry> getAt(int index) {
        if (index < 0 || index >= entries.size()) {
            return Optional.empty();
        }
        return Optional.of(entries.get(index));
    }

    public boolean setWeightAt(int index, int weight) {
        Optional<WeightedPrefabEntry> entry = getAt(index);
        if (entry.isEmpty()) {
            return false;
        }
        WeightedPrefabEntry old = entry.get();
        entries.set(index, new WeightedPrefabEntry(old.prefabPath(), old.displayName(), weight));
        return true;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        if (index < 0 || index >= entries.size()) {
            selectedIndex = entries.isEmpty() ? -1 : 0;
            return;
        }
        selectedIndex = index;
    }

    @Nullable
    public WeightedPrefabEntry selectedEntry() {
        if (selectedIndex < 0 || selectedIndex >= entries.size()) {
            return null;
        }
        return entries.get(selectedIndex);
    }

    public int totalWeight() {
        int total = 0;
        for (WeightedPrefabEntry entry : entries) {
            total += entry.weight();
        }
        return total;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        MultiPrefabPasteState copy = new MultiPrefabPasteState();
        for (WeightedPrefabEntry entry : entries) {
            copy.entries.add(new WeightedPrefabEntry(entry.prefabPath(), entry.displayName(), entry.weight()));
        }
        copy.selectedIndex = selectedIndex;
        return copy;
    }
}
