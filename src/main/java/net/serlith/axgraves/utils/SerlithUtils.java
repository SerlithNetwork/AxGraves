package net.serlith.axgraves.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@NullMarked
public class SerlithUtils {

    public static final ConcurrentMap<UUID, Location> DEATH_LOCATIONS = new ConcurrentHashMap<>();

    public static boolean isNotRespawnCompass(@Nullable ItemStack item) {
        return !SerlithUtils.isRespawnCompass(item);
    }

    public static boolean isRespawnCompass(@Nullable ItemStack item) {
        return item != null && !item.isEmpty() && item.getType() == Material.COMPASS && item.getPersistentDataContainer().has(KeyUtils.RESPAWN_COMPASS);
    }

    public static boolean isHelmet(Material material) {
        return Tag.ITEMS_ENCHANTABLE_HEAD_ARMOR.isTagged(material) || material.equals(Material.TURTLE_HELMET);
    }

    public static boolean isChestplate(Material material) {
        return Tag.ITEMS_ENCHANTABLE_CHEST_ARMOR.isTagged(material) || material.equals(Material.ELYTRA);
    }

    public static boolean isLeggings(Material material) {
        return Tag.ITEMS_ENCHANTABLE_LEG_ARMOR.isTagged(material);
    }

    public static boolean isBoots(Material material) {
        return Tag.ITEMS_ENCHANTABLE_FOOT_ARMOR.isTagged(material);
    }

}
