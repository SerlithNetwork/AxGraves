package com.artillexstudios.axgraves.utils;

import com.artillexstudios.axapi.nms.wrapper.ServerPlayerWrapper;
import com.artillexstudios.axapi.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import static com.artillexstudios.axgraves.AxGraves.CONFIG;

public class Utils {

    @NotNull
    public static ItemStack getPlayerHead(@NotNull OfflinePlayer player) {
        ItemBuilder builder = ItemBuilder.create(Material.PLAYER_HEAD);

        String texture = null;
        if (CONFIG.getBoolean("custom-grave-skull.enabled", false)) {
            texture = CONFIG.getString("custom-grave-skull.base64");
        } else if (player.getPlayer() != null) {
            ServerPlayerWrapper wrapper = ServerPlayerWrapper.wrap(player);
            texture = wrapper.textures().texture();
        }

        if (texture != null) builder.setTextureValue(texture);

        return builder.get();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isRespawnCompass(@NotNull ItemStack item) {
        return !item.isEmpty() && item.getType() == Material.COMPASS && item.getPersistentDataContainer().getOrDefault(KeyUtils.RESPAWN_COMPASS, PersistentDataType.BOOLEAN, false);
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
