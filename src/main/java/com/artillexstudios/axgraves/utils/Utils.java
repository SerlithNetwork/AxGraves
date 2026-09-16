package com.artillexstudios.axgraves.utils;

import com.artillexstudios.axapi.items.WrappedItemStack;
import com.artillexstudios.axapi.nms.wrapper.ServerPlayerWrapper;
import com.artillexstudios.axapi.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.artillexstudios.axgraves.AxGraves.CONFIG;

public class Utils {

    @NotNull
    public static WrappedItemStack getPlayerHead(@Nullable String texture) {
        ItemBuilder builder = ItemBuilder.create(Material.PLAYER_HEAD);
        if (texture != null) {
            builder.setTextureValue(texture);
        }
        return builder.wrapped();
    }

    @NotNull
    public static String getTexture(@NotNull Player player) {
        String texture;
        if (CONFIG.getBoolean("custom-grave-skull.enabled", false)) {
            texture = CONFIG.getString("custom-grave-skull.base64");
        } else {
            ServerPlayerWrapper wrapper = ServerPlayerWrapper.wrap(player);
            texture = wrapper.textures().texture();
        }
        return texture;
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
