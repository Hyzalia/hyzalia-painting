package com.hyzalia.paint.paste;

/** Constantes partagées pour l'outil paste multi-prefab Hyzalia. */
public final class HyzaliaPasteToolConstants {

    /** Id BuilderTool côté client (preview Paste vanilla) ; détection serveur par {@link #ITEM_ID}. */
    public static final String BUILDER_TOOL_ID = "Paste";

    /** Identifiant item éditeur (EditorTool_*.json sans préfixe EditorTool_). */
    public static final String ITEM_ID = "EditorTool_Hyzalia_PasteTool";

    /** Commandes chat déclenchées par le bouton Add Prefab du panel (fallback client). */
    public static final String PREFAB_LOAD_COMMAND = "/prefab load";
    public static final String HYZALIA_ADD_COMMAND = "/hyzaliapaste add";
    public static final String HYZALIA_LOAD_ALIAS = "/hyzaliapaste load";

    /** Poids par défaut lors de l'ajout d'une prefab à la liste. */
    public static final int DEFAULT_WEIGHT = 100;

    private HyzaliaPasteToolConstants() {
    }
}
