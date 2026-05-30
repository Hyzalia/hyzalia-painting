package com.hyzalia.paint.paste;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.InventorySetActiveSlotEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/** Met à jour le HUD pool prefab quand le joueur change de slot hotbar. */
public final class HyzaliaPastePoolHudSystem
        extends EntityEventSystem<EntityStore, InventorySetActiveSlotEvent> {

    public HyzaliaPastePoolHudSystem() {
        super(InventorySetActiveSlotEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), PlayerRef.getComponentType());
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull InventorySetActiveSlotEvent event) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            return;
        }

        if (HyzaliaPasteToolItems.isHoldingHyzaliaPasteTool(ref, store)) {
            HyzaliaPastePoolHud.showOrRefresh(player, playerRef, ref, store);
        } else {
            HyzaliaPastePoolHud.hideIfPresent(player, playerRef);
        }
    }
}
