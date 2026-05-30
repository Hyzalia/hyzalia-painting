package com.hyzalia.paint.paste;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPacketHandler;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolPasteClipboard;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolRandomizeClipboard;
import com.hypixel.hytale.server.core.asset.type.buildertool.config.BuilderTool;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.io.handlers.IPacketHandler;
import com.hypixel.hytale.server.core.io.handlers.IWorldPacketHandler;
import com.hypixel.hytale.server.core.io.handlers.SubPacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Handlers réseau pour {@link HyzaliaPasteToolConstants#ITEM_ID} :
 * paste/randomize uniquement si l'item Hyzalia ou le Paste vanilla est en main.
 */
public final class HyzaliaPasteToolPacketHandler implements SubPacketHandler {

    private static final String VANILLA_PASTE_TOOL_ID = "Paste";

    private final IPacketHandler packetHandler;
    private final BuilderToolsPacketHandler vanillaHandler;

    public HyzaliaPasteToolPacketHandler(@Nonnull IPacketHandler packetHandler) {
        this.packetHandler = Objects.requireNonNull(packetHandler, "packetHandler");
        this.vanillaHandler = new BuilderToolsPacketHandler(packetHandler);
    }

    @Override
    public void registerHandlers() {
        if (com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin.get().isDisabled()) {
            return;
        }

        IWorldPacketHandler.registerHandler(
                packetHandler,
                BuilderToolPasteClipboard.PACKET_ID,
                this::handlePasteClipboard);

        IWorldPacketHandler.registerHandler(
                packetHandler,
                BuilderToolRandomizeClipboard.PACKET_ID,
                this::handleRandomizeClipboard);
    }

    private void handlePasteClipboard(
            @Nonnull BuilderToolPasteClipboard packet,
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull World world,
            @Nonnull Store<EntityStore> store) {
        if (HyzaliaPasteHeldTool.getHeld(ref, store).isPresent()) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                return;
            }
            MultiPrefabPasteState state = MultiPrefabPasteState.ensure(ref, store);
            HyzaliaPasteToolService.pasteAt(player, playerRef, ref, state, packet.x, packet.y, packet.z, store);
            return;
        }
        if (isVanillaPasteToolInHand(ref, store)) {
            vanillaHandler.handleBuilderToolPasteClipboard(packet, playerRef, ref, world, store);
        }
    }

    private void handleRandomizeClipboard(
            @Nonnull BuilderToolRandomizeClipboard packet,
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull World world,
            @Nonnull Store<EntityStore> store) {
        if (HyzaliaPasteHeldTool.getHeld(ref, store).isPresent()) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                return;
            }
            MultiPrefabPasteState state = MultiPrefabPasteState.ensure(ref, store);
            HyzaliaPasteToolService.applyRandomizeFromPacket(player, playerRef, ref, state, packet, store);
            return;
        }
        if (isVanillaPasteToolInHand(ref, store)) {
            vanillaHandler.handleBuilderToolRandomizeClipboard(packet, playerRef, ref, world, store);
        }
    }

    private static boolean isVanillaPasteToolInHand(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store) {
        ItemStack stack = InventoryComponent.getItemInHand(store, ref);
        if (stack == null || stack.getItem() == null || HyzaliaPasteToolItems.isHyzaliaPasteItem(stack)) {
            return false;
        }
        BuilderTool tool = stack.getItem().getBuilderTool();
        return tool != null && VANILLA_PASTE_TOOL_ID.equals(tool.getId());
    }
}
