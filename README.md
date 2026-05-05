# Hyzalia Paint - LayersStratum Tool

---

## FR - Presentation

`Hyzalia Paint` est un plugin serveur Java pour Hytale qui ajoute un outil BuilderTools nomme `LayersStratum`.

Cet outil sert a peindre des couches de materiaux avec controle fin des transitions (RadUp/RadDown), de la densite, et du mode d'application (surface locale ou strate monde).

## FR - Fonctionnement rapide

- Jusqu'a 6 couches configurables.
- Chaque couche peut definir:
  - un materiau (`Layer N material`)
  - une epaisseur (`Layer N thickness`)
  - un gradient vers le haut (`Layer N gradient up rows/chance`)
  - un gradient vers le bas (`Layer N gradient down rows/chance`)
  - un toggle d'activation (`Enable Layer N`, pour les couches 2 a 6)
  - un skip (`Skip Layer N`)
- Direction du brush configurable: `Up`, `Down`, `Camera`, `North`, `South`, `East`, `West`.
- Densite globale: `Brush density (%)`.

## FR - Modes: quoi activer pour faire quoi

### 1) Faire des strates "autour du point peint" (cas standard)

Reglages a utiliser (noms affiches dans l'outil):

- desactiver `World stratum mode (horizontal bands by Y)`
- choisir `Direction = Down` (souvent le plus naturel pour du terrain)
- regler `Layer 1 thickness`, `Layer 2 thickness`, etc.
- mettre `Brush density (%) = 100` pour un rendu plein

Resultat:

- Le plugin construit une colonne de couches depuis chaque bloc touche par le brush.

### 2) Faire des bandes geologiques fixes par altitude (meme Y partout)

Reglages a activer (noms affiches dans l'outil):

- activer `World stratum mode (horizontal bands by Y)`
- definir `Stratum top Y (highest block row, toward sky)`
- definir `Stratum bottom Y (lowest block row)`

Ajustements conseilles:

- Garder des valeurs d'epaisseur sur les couches utiles (`Layer N thickness`).
- Utiliser `Enable Layer N` pour reduire les couches actives.
- Les epaisseurs deviennent des "poids" sur la hauteur totale de la bande.

Resultat:

- La matiere depend de l'altitude globale (`Y`) et non de la hauteur locale du terrain.

### 3) Faire des bords plus organiques

Reglages a utiliser:

- `Layer N gradient up rows` + `Layer N gradient up chance (%)`
- `Layer N gradient down rows` + `Layer N gradient down chance (%)`

Exemple simple:

- `gradient up rows = 3`, `gradient up chance = 50`
- `gradient down rows = 2`, `gradient down chance = 35`

Resultat:

- Le haut et le bas des couches deviennent moins "coupes nettes".

### 4) Eviter les couches qui parasitent

Reglages a verifier:

- `Skip Layer N = true` pour ignorer une couche sans perdre la configuration.
- `Enable Layer N = false` (couches 2 a 6) pour la desactiver completement.
- Materiau vide ou invalide = couche ignoree dans les faits.

## FR - Reglages visibles dans l'interface

- `Direction`
- `World stratum mode (horizontal bands by Y)`
- `Stratum top Y (highest block row, toward sky)`
- `Stratum bottom Y (lowest block row)`
- `Layer N thickness`
- `Layer N material`
- `Layer N gradient up rows`
- `Layer N gradient up chance (%)`
- `Layer N gradient down rows`
- `Layer N gradient down chance (%)`
- `Enable Layer N`
- `Skip Layer N`
- `Brush density (%)`

---

## EN - Overview

`Hyzalia Paint` is a Java server plugin for Hytale that adds a BuilderTools operation called `LayersStratum`.

The tool paints layered materials with controlled transitions (RadUp/RadDown), density filtering, and two application modes (local surface or global world stratum).

## EN - Quick behavior

- Up to 6 configurable layers.
- Each layer supports:
  - material (`Layer N material`)
  - thickness (`Layer N thickness`)
  - upward gradient (`Layer N gradient up rows/chance`)
  - downward gradient (`Layer N gradient down rows/chance`)
  - activation toggle (`Enable Layer N`, layers 2 to 6)
  - skip toggle (`Skip Layer N`)
- Brush direction: `Up`, `Down`, `Camera`, `North`, `South`, `East`, `West`.
- Global density filter: `Brush density (%)`.

## EN - Modes: which toggles for which result

### 1) Paint local stacked layers (default use case)

Set these UI options:

- disable `World stratum mode (horizontal bands by Y)`
- choose `Direction = Down` (common for terrain layering)
- adjust `Layer 1 thickness`, `Layer 2 thickness`, etc.
- set `Brush density (%) = 100` for full coverage

Result:

- The tool builds a local material column from each touched block.

### 2) Paint fixed altitude bands across the world

Enable and set these UI options:

- `World stratum mode (horizontal bands by Y)`
- `Stratum top Y (highest block row, toward sky)`
- `Stratum bottom Y (lowest block row)`

Recommended:

- Keep non-zero `Layer N thickness` values on active layers.
- Use `Enable Layer N` to limit active layers.
- Layer thickness is treated as weight over the full band height.

Result:

- Material selection is driven by global altitude (`Y`), not local terrain height.

### 3) Create softer/organic boundaries

Tune:

- `Layer N gradient up rows` + `Layer N gradient up chance (%)`
- `Layer N gradient down rows` + `Layer N gradient down chance (%)`

Result:

- Layer boundaries become less sharp and more natural.

### 4) Disable noisy layers quickly

Use:

- `Skip Layer N = true` to temporarily ignore a layer
- `Enable Layer N = false` (layers 2..6) to fully disable it

## EN - Main UI labels

- `Direction`
- `World stratum mode (horizontal bands by Y)`
- `Stratum top Y (highest block row, toward sky)`
- `Stratum bottom Y (lowest block row)`
- `Layer N thickness`
- `Layer N material`
- `Layer N gradient up rows`
- `Layer N gradient up chance (%)`
- `Layer N gradient down rows`
- `Layer N gradient down chance (%)`
- `Enable Layer N`
- `Skip Layer N`
- `Brush density (%)`

---

## Build & install

### Build

```bash
# Main plugin jar
./gradlew shadowJar

# Main plugin + early-bridge jar
./gradlew build
```

### Runtime placement

- Put `hyzalia-paint-*.jar` in `mods/`.
- Put `HyzaliaPaint-EarlyBridge-execute0-*.jar` in `earlyplugins/`.
- Start server with `--accept-early-plugins`.

---

## Early bridge (important)

The repository includes an `early-bridge` subproject with a `ClassTransformer` that makes `ToolOperation#execute0` public at load time.

This avoids runtime visibility/classloader issues for the BuilderTools bridge operation.

---

## License

This project is licensed under `CC BY-NC 4.0`:

- public use and sharing allowed
- commercial resale/use is not allowed without explicit permission
