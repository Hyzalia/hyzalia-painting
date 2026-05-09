package com.hyzalia.paint.prefab;

import com.hypixel.hytale.builtin.buildertools.prefablist.PrefabSavePage;
import com.hypixel.hytale.server.core.ui.browser.AssetPackSaveBrowser;

import java.lang.reflect.Field;

/**
 * Accès au navigateur de packs de {@link PrefabSavePage} : champ {@code packBrowser} privé côté vanilla,
 * nécessaire pour répliquer {@code handleDataEvent} avec une sauvegarde enrichie (fluides).
 */
final class PrefabSavePageAccess {

    private static final Field PACK_BROWSER_FIELD;

    static {
        try {
            PACK_BROWSER_FIELD = PrefabSavePage.class.getDeclaredField("packBrowser");
            PACK_BROWSER_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private PrefabSavePageAccess() {
    }

    static AssetPackSaveBrowser packBrowser(PrefabSavePage page) {
        try {
            return (AssetPackSaveBrowser) PACK_BROWSER_FIELD.get(page);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
