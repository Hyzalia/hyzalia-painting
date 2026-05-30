package com.hyzalia.paint.paste;

import com.hypixel.hytale.builtin.buildertools.prefablist.PrefabPage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Rafraîchit la liste prefab sans ré-appender le shell UI (évite les menus empilés). */
public final class PrefabPageListingRefresh {

    private static final Method SEND_LISTING_UPDATE = resolveSendListingUpdate();

    private PrefabPageListingRefresh() {
    }

    public static void refresh(PrefabPage page) {
        if (SEND_LISTING_UPDATE == null) {
            return;
        }
        try {
            SEND_LISTING_UPDATE.invoke(page);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Impossible de rafraîchir la liste prefab", ex);
        }
    }

    private static Method resolveSendListingUpdate() {
        try {
            Method method = PrefabPage.class.getDeclaredMethod("sendListingUpdate");
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }
}
