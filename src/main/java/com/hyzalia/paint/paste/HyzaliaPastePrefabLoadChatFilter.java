package com.hyzalia.paint.paste;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.ChatMessage;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Intercepte {@code /prefab load} (bouton panel) lorsque l'outil Hyzalia est en main
 * et ouvre le navigateur Add Prefab — sans early-bridge ASM.
 */
public final class HyzaliaPastePrefabLoadChatFilter implements PlayerPacketFilter {

    @Nullable
    private static PacketFilter registration;

    private HyzaliaPastePrefabLoadChatFilter() {
    }

    public static void register() {
        if (registration != null) {
            return;
        }
        HyzaliaPastePrefabLoadChatFilter filter = new HyzaliaPastePrefabLoadChatFilter();
        registration = com.hypixel.hytale.server.core.io.adapter.PacketAdapters.registerInbound(filter);
    }

    public static void unregister() {
        if (registration != null) {
            com.hypixel.hytale.server.core.io.adapter.PacketAdapters.deregisterInbound(registration);
            registration = null;
        }
    }

    @Override
    public boolean test(@Nonnull PlayerRef playerRef, @Nonnull com.hypixel.hytale.protocol.Packet packet) {
        if (!(packet instanceof ChatMessage chat)) {
            return true;
        }
        if (!isAddPrefabCommand(chat.message)) {
            return true;
        }
        if (!HyzaliaPasteToolItems.isHoldingHyzaliaPasteTool(playerRef)) {
            return true;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return true;
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        if (world.isInThread()) {
            HyzaliaPasteUiActions.openAddPrefabPage(ref, store, playerRef);
        } else {
            world.execute(() -> HyzaliaPasteUiActions.openAddPrefabPage(ref, store, playerRef));
        }
        return false;
    }

    static boolean isAddPrefabCommand(@Nullable String rawMessage) {
        if (rawMessage == null) {
            return false;
        }
        String message = rawMessage.trim().toLowerCase(Locale.ROOT);
        return message.equals(HyzaliaPasteToolConstants.PREFAB_LOAD_COMMAND)
                || message.equals(HyzaliaPasteToolConstants.HYZALIA_ADD_COMMAND)
                || message.equals(HyzaliaPasteToolConstants.HYZALIA_LOAD_ALIAS)
                || message.equals("prefab load");
    }
}
