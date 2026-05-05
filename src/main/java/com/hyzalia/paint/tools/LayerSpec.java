package com.hyzalia.paint.tools;

/**
 * Description figée d'une couche du brush {@code LayersStratum}.
 *
 * <p>Une couche est <strong>active</strong> si elle est activée par l'utilisateur
 * (la couche 1 l'est implicitement) et n'est pas marquée « skip ».
 *
 * @param material        identifiant de bloc / motif (ex. {@code "Soil_Grass_Full"})
 * @param thickness       en mode surface : nombre de rangées ; en mode strate monde :
 *                          poids (avec les autres couches actives) pour répartir la hauteur
 *                          {@code topY - bottomY + 1} entre les couches
 * @param radUpRows       nombre de rangées de dégradé vers la couche au-dessus
 * @param radUpChance     probabilité (0–100) qu'une rangée du dégradé haut prenne ce matériau
 * @param radDownRows     nombre de rangées de dégradé vers la couche en-dessous
 * @param radDownChance   probabilité (0–100) qu'une rangée du dégradé bas prenne ce matériau
 * @param enabled         {@code true} si la couche est activée par l'utilisateur
 * @param skip            {@code true} si la couche doit être ignorée pour ce coup de brush
 */
public record LayerSpec(
        String material,
        int thickness,
        int radUpRows,
        int radUpChance,
        int radDownRows,
        int radDownChance,
        boolean enabled,
        boolean skip) {

    /** {@code true} si la couche doit participer à la composition de la colonne. */
    public boolean active() {
        return enabled && !skip && thickness > 0 && material != null && !material.isEmpty();
    }
}
