package com.artillexstudios.axgraves.listeners;

import com.artillexstudios.axgraves.AxGraves;
import com.artillexstudios.axgraves.utils.KeyUtils;
import com.artillexstudios.axgraves.utils.LocationUtils;
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

import static com.artillexstudios.axgraves.AxGraves.CONFIG;

public class RespawnListener implements Listener {

    private static boolean RESPAWN_TITLE_ENABLED;
    private static String RESPAWN_TITLE_MESSAGE;
    private static long RESPAWN_TITLE_DURATION_FADE_IN;
    private static long RESPAWN_TITLE_DURATION_STAY;
    private static long RESPAWN_TITLE_DURATION_FADE_OUT;
    private static long RESPAWN_TITLE_DELAY;

    private static boolean RESPAWN_COMPASS_ENABLED;
    private static String RESPAWN_COMPASS_DISPLAY_NAME;
    private static List<String> RESPAWN_COMPASS_LORE;

    public static void reload() {
        RESPAWN_TITLE_ENABLED = CONFIG.getBoolean("respawn-title.enabled", false);
        RESPAWN_TITLE_MESSAGE = CONFIG.getString("respawn-title.message", "");
        RESPAWN_TITLE_DURATION_FADE_IN = CONFIG.getLong("respawn-title.duration.fade-in", 0L);
        RESPAWN_TITLE_DURATION_STAY = CONFIG.getLong("respawn-title.duration.stay", 200L);
        RESPAWN_TITLE_DURATION_FADE_OUT = CONFIG.getLong("respawn-title.duration.fade-out", 0L);
        RESPAWN_TITLE_DELAY = CONFIG.getLong("respawn-title.delay", 40L);

        RESPAWN_COMPASS_ENABLED = CONFIG.getBoolean("respawn-compass.enabled", false);
        RESPAWN_COMPASS_DISPLAY_NAME = CONFIG.getString("respawn-compass.display-name", "");
        RESPAWN_COMPASS_LORE = CONFIG.getStringList("respawn-compass.lore", List.of());
    }

    public RespawnListener() {
        reload();
        AxGraves.getInstance().getServer().getPluginManager().registerEvents(this, AxGraves.getInstance());
    }

    @EventHandler
    public void onRespawn(@NotNull PlayerPostRespawnEvent event) {
        final Player player = event.getPlayer();

        if (RESPAWN_TITLE_ENABLED) {
            final String title = RESPAWN_TITLE_MESSAGE;
            final long fadeIn = RESPAWN_TITLE_DURATION_FADE_IN;
            final long stay = RESPAWN_TITLE_DURATION_STAY;
            final long fadeOut = RESPAWN_TITLE_DURATION_FADE_OUT;
            Bukkit.getScheduler().runTaskLaterAsynchronously(AxGraves.getInstance(), () -> {
                Location location = LocationUtils.DEATH_LOCATIONS.get(player.getUniqueId());
                if (location == null) {
                    return;
                }

                player.showTitle(
                        Title.title(
                                MiniMessage.miniMessage().deserialize(title,
                                        Placeholder.unparsed("x", String.format("%d", location.getBlockX())),
                                        Placeholder.unparsed("y", String.format("%d", location.getBlockY())),
                                        Placeholder.unparsed("z", String.format("%d", location.getBlockZ()))
                                ),
                                Component.empty(),
                                Title.Times.times(
                                        Duration.ofMillis(fadeIn),
                                        Duration.ofMillis(stay),
                                        Duration.ofMillis(fadeOut)
                                )
                        )
                );
            }, RESPAWN_TITLE_DELAY);
        }

        if (RESPAWN_COMPASS_ENABLED) {
            Location location = LocationUtils.DEATH_LOCATIONS.get(player.getUniqueId());
            if (location != null) {
                String rawDisplayName = RESPAWN_COMPASS_DISPLAY_NAME;
                List<String> rawLore = RESPAWN_COMPASS_LORE;
                Bukkit.getScheduler().runTaskAsynchronously(AxGraves.getInstance(), () -> {
                    World world = location.getWorld();
                    String worldName = LocationUtils.getWorldName(world);
                    ItemStack compass = ItemStack.of(Material.COMPASS, 1);
                    CompassMeta meta = (CompassMeta) compass.getItemMeta();
                    meta.setLodestone(location.clone());
                    meta.setLodestoneTracked(false);
                    meta.displayName(MiniMessage.miniMessage().deserialize(rawDisplayName).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
                    meta.lore(rawLore.stream().map(s -> MiniMessage.miniMessage().deserialize(s,
                                    Placeholder.unparsed("player", event.getPlayer().getName()),
                                    Placeholder.component("face", Component.object(ObjectContents.playerHead(event.getPlayer().getName()))),
                                    Placeholder.unparsed("world", worldName),
                                    Placeholder.unparsed("x", String.format("%.0f", location.x())),
                                    Placeholder.unparsed("y", String.format("%.0f", location.y())),
                                    Placeholder.unparsed("z", String.format("%.0f", location.z()))
                            ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)).toList()
                    );
                    meta.addEnchant(Enchantment.UNBREAKING, 1, false);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    compass.setItemMeta(meta);
                    compass.editPersistentDataContainer(pdc -> pdc.set(KeyUtils.RESPAWN_COMPASS, PersistentDataType.BOOLEAN, true));
                    Bukkit.getScheduler().runTask(AxGraves.getInstance(), () -> event.getPlayer().getInventory().addItem(compass));
                });
            }
        }

    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        LocationUtils.DEATH_LOCATIONS.remove(event.getPlayer().getUniqueId());
    }

}
