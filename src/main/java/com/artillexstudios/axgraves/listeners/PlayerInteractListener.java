package com.artillexstudios.axgraves.listeners;

import com.artillexstudios.axapi.packet.wrapper.serverbound.ServerboundInteractWrapper;
import com.artillexstudios.axgraves.grave.Grave;
import com.artillexstudios.axgraves.grave.SpawnedGraves;
import com.artillexstudios.axgraves.utils.KeyUtils;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class PlayerInteractListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getHand() == null) return;

        ServerboundInteractWrapper.InteractionHand hand = switch (event.getHand()) {
            case HAND -> ServerboundInteractWrapper.InteractionHand.MAIN_HAND;
            case OFF_HAND -> ServerboundInteractWrapper.InteractionHand.OFF_HAND;
            default -> null;
        };
        if (hand == null) return;

        if (event.getClickedBlock().getType() == Material.LODESTONE) { // Don't re-use compasses
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.COMPASS && item.getPersistentDataContainer().getOrDefault(KeyUtils.RESPAWN_COMPASS, PersistentDataType.BOOLEAN, false)) {
                event.setCancelled(true);
            }
        }

        for (Grave grave : SpawnedGraves.getGraves()) {
            if (!grave.getLocation().getBlock().equals(event.getClickedBlock())) continue;
            grave.interact(event.getPlayer(), hand);
            return;
        }
    }
}