package com.hyzalia.paint.paste;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import javax.annotation.Nonnull;
import java.util.Objects;

/** Entrée prefab avec poids relatif pour la sélection aléatoire. */
public final class WeightedPrefabEntry {

    public static final BuilderCodec<WeightedPrefabEntry> CODEC = BuilderCodec.builder(
                    WeightedPrefabEntry.class,
                    WeightedPrefabEntry::new)
            .append(new KeyedCodec<>("Path", Codec.STRING), (entry, value) -> entry.prefabPath = value, entry -> entry.prefabPath)
            .add()
            .append(new KeyedCodec<>("DisplayName", Codec.STRING), (entry, value) -> entry.displayName = value, entry -> entry.displayName)
            .add()
            .append(new KeyedCodec<>("Weight", Codec.INTEGER), (entry, value) -> entry.weight = Math.max(1, value), entry -> entry.weight)
            .add()
            .build();

    private String prefabPath = "";
    private String displayName = "";
    private int weight = HyzaliaPasteToolConstants.DEFAULT_WEIGHT;

    public WeightedPrefabEntry() {
    }

    public WeightedPrefabEntry(@Nonnull String prefabPath, @Nonnull String displayName, int weight) {
        this.prefabPath = Objects.requireNonNull(prefabPath, "prefabPath");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.weight = Math.max(1, weight);
    }

    @Nonnull
    public String prefabPath() {
        return prefabPath;
    }

    @Nonnull
    public String displayName() {
        return displayName;
    }

    public int weight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = Math.max(1, weight);
    }

    /** Clé utilisée par {@link com.hypixel.hytale.server.core.prefab.PrefabWeights}. */
    @Nonnull
    public String weightKey() {
        return prefabPath + "#" + displayName;
    }
}
