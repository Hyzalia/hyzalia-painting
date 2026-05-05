package com.hyzalia.paint.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Construit la séquence de matériaux pour le brush {@code LayersStratum}.
 *
 * <p>Deux modes de hauteur de colonne :
 * <ul>
 *   <li><strong>Surface</strong> : une rangée par unité de {@link LayerSpec#thickness()} ;</li>
 *   <li><strong>Strate monde</strong> : hauteur fixe {@code bandHeight} ; chaque {@code thickness}
 *       est un <em>poids</em> ; les hauteurs en blocs sont {@code floor(bandHeight * w_i / sum(w))}
 *       puis le reste est réparti par la méthode du plus grand reste.</li>
 * </ul>
 *
 * <p>Ensuite, pour les deux modes, dégradés RadDown / RadUp entre couches voisines (même logique).
 */
public final class LayersGradientBuilder {

    private LayersGradientBuilder() {}

    /**
     * Mode surface : une rangée par unité d'épaisseur de chaque couche active.
     */
    public static List<String> buildColumn(List<LayerSpec> active, Random rng) {
        if (active == null || active.isEmpty()) {
            return new ArrayList<>(0);
        }
        int[] heights = new int[active.size()];
        for (int i = 0; i < active.size(); i++) {
            heights[i] = Math.max(0, active.get(i).thickness());
        }
        int total = 0;
        for (int h : heights) {
            total += h;
        }
        if (total <= 0) {
            return new ArrayList<>(0);
        }
        return buildColumnFromLayerHeights(active, heights, rng);
    }

    /**
     * Mode strate monde : hauteur totale {@code bandHeight} ; les {@link LayerSpec#thickness()}
     * des couches actives sont des poids entiers (somme &gt; 0).
     *
     * @param bandHeight nombre de rangées (typiquement {@code topY - bottomY + 1})
     */
    public static List<String> buildColumnFromWeights(List<LayerSpec> active, int bandHeight, Random rng) {
        if (active == null || active.isEmpty() || bandHeight <= 0) {
            return new ArrayList<>(0);
        }
        int[] weights = new int[active.size()];
        for (int i = 0; i < active.size(); i++) {
            weights[i] = Math.max(0, active.get(i).thickness());
        }
        int[] heights = allocateWeightedHeights(weights, bandHeight);
        int total = 0;
        for (int h : heights) {
            total += h;
        }
        if (total <= 0) {
            return new ArrayList<>(0);
        }
        return buildColumnFromLayerHeights(active, heights, rng);
    }

    /**
     * Répartition des {@code bandHeight} rangées entre {@code n} couches de poids {@code weights[i]}.
     * Méthode du plus grand reste après {@code floor}.
     */
    public static int[] allocateWeightedHeights(int[] weights, int bandHeight) {
        if (weights == null || weights.length == 0 || bandHeight <= 0) {
            return new int[0];
        }
        int n = weights.length;
        long sumW = 0L;
        for (int w : weights) {
            sumW += Math.max(0, w);
        }
        int[] result = new int[n];
        if (sumW <= 0L) {
            int base = bandHeight / n;
            int rem = bandHeight % n;
            for (int i = 0; i < n; i++) {
                result[i] = base + (i < rem ? 1 : 0);
            }
            return result;
        }
        double[] exact = new double[n];
        int sumFloor = 0;
        for (int i = 0; i < n; i++) {
            double wi = Math.max(0, weights[i]);
            exact[i] = bandHeight * wi / (double) sumW;
            result[i] = (int) Math.floor(exact[i]);
            sumFloor += result[i];
        }
        int leftover = bandHeight - sumFloor;
        if (leftover <= 0) {
            return result;
        }
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) {
            idx[i] = i;
        }
        Arrays.sort(idx, Comparator.comparingDouble((Integer i) -> exact[i] - result[i]).reversed());
        for (int k = 0; k < leftover; k++) {
            result[idx[k]]++;
        }
        return result;
    }

    private static List<String> buildColumnFromLayerHeights(
            List<LayerSpec> active, int[] heights, Random rng) {
        int n = active.size();
        int total = 0;
        for (int h : heights) {
            total += h;
        }
        if (total <= 0) {
            return new ArrayList<>(0);
        }

        int[] starts = new int[n];
        int acc = 0;
        for (int i = 0; i < n; i++) {
            starts[i] = acc;
            acc += heights[i];
        }

        String[] rows = new String[total];
        for (int i = 0; i < n; i++) {
            LayerSpec layer = active.get(i);
            int start = starts[i];
            int end = i + 1 < n ? starts[i + 1] : total;
            for (int r = start; r < end; r++) {
                rows[r] = layer.material();
            }
        }

        for (int i = 0; i < n - 1; i++) {
            LayerSpec layer = active.get(i);
            int radDown = Math.max(0, layer.radDownRows());
            if (radDown == 0) {
                continue;
            }
            int chance = clampPercent(layer.radDownChance());
            int boundary = starts[i + 1];
            for (int j = 0; j < radDown && boundary + j < total; j++) {
                if (rollChance(rng, chance)) {
                    rows[boundary + j] = layer.material();
                }
            }
        }

        for (int i = 1; i < n; i++) {
            LayerSpec layer = active.get(i);
            int radUp = Math.max(0, layer.radUpRows());
            if (radUp == 0) {
                continue;
            }
            int chance = clampPercent(layer.radUpChance());
            int boundary = starts[i];
            for (int j = 1; j <= radUp && boundary - j >= 0; j++) {
                if (rollChance(rng, chance)) {
                    rows[boundary - j] = layer.material();
                }
            }
        }

        List<String> out = new ArrayList<>(rows.length);
        Collections.addAll(out, rows);
        return out;
    }

    /**
     * Tire pour chacune des {@code rows} rangées d'extension hors colonne (surface UP
     * pour la couche 1, ou depth DOWN pour la dernière) si oui ou non on pose le bloc.
     * Les rangées non posées restent à l'air libre / au bloc d'origine.
     *
     * @param rows   nombre de rangées à poser
     * @param chance probabilité (0–100) de poser le matériau pour chaque rangée
     * @param rng    générateur aléatoire
     * @return tableau de booléens de longueur {@code rows} ({@code true} = poser)
     */
    public static boolean[] rollExtensionMask(int rows, int chance, Random rng) {
        if (rows <= 0) {
            return new boolean[0];
        }
        int chancePercent = clampPercent(chance);
        boolean[] mask = new boolean[rows];
        for (int i = 0; i < rows; i++) {
            mask[i] = rollChance(rng, chancePercent);
        }
        return mask;
    }

    private static int clampPercent(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 100);
    }

    private static boolean rollChance(Random rng, int chancePercent) {
        if (chancePercent <= 0) {
            return false;
        }
        if (chancePercent >= 100) {
            return true;
        }
        return rng.nextInt(100) < chancePercent;
    }
}
