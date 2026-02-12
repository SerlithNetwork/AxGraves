package com.artillexstudios.axgraves.listeners;

import com.artillexstudios.axapi.utils.logging.LogUtils;
import com.artillexstudios.axgraves.AxGraves;
import com.artillexstudios.axgraves.api.events.GravePreSpawnEvent;
import com.artillexstudios.axgraves.api.events.GraveSpawnEvent;
import com.artillexstudios.axgraves.grave.Grave;
import com.artillexstudios.axgraves.grave.SpawnedGraves;
import com.artillexstudios.axgraves.utils.ExperienceUtils;
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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.bukkit.plugin.EventExecutor;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.artillexstudios.axgraves.AxGraves.CONFIG;

public class DeathListener implements Listener {
    private static List<String> disabledWorlds;
    private static List<String> blacklistedDeathCauses;
    private static boolean overrideKeepInventory;
    private static boolean overrideKeepLevel;
    private static boolean storeItems;
    private static boolean storeXP;
    private static float xpKeepPercentage;
    private static boolean softKeepInventory;
    private static float softInventoryPercentage;
    private static float softPityPercentage;
    private static boolean softPityArmor;
    private static boolean softPityMainHand;
    private static boolean softPityOffHand;

    public static void reload() {
        disabledWorlds = CONFIG.getStringList("disabled-worlds");
        blacklistedDeathCauses = CONFIG.getStringList("blacklisted-death-causes");
        overrideKeepInventory = CONFIG.getBoolean("override-keep-inventory", true);
        overrideKeepLevel = CONFIG.getBoolean("override-keep-level", true);
        storeItems = CONFIG.getBoolean("store-items", true);
        storeXP = CONFIG.getBoolean("store-xp", true);
        xpKeepPercentage = CONFIG.getFloat("xp-keep-percentage", 1f);
        softKeepInventory = CONFIG.getBoolean("soft-keep-inventory.enabled", false);
        softInventoryPercentage = CONFIG.getFloat("soft-keep-inventory.percentage", 0.5f);
        softPityPercentage = CONFIG.getFloat("soft-keep-inventory.pity.percentage", 0.8f);
        softPityArmor = CONFIG.getBoolean("soft-keep-inventory.pity.slots.armor-contents", true);
        softPityMainHand = CONFIG.getBoolean("soft-keep-inventory.pity.slots.main-hand", true);
        softPityOffHand = CONFIG.getBoolean("soft-keep-inventory.pity.slots.off-hand", true);
    }

    public DeathListener() {
        reload();

        String priority = CONFIG.getString("death-listener-priority", "MONITOR");
        EventPriority eventPriority;
        try {
            eventPriority = EventPriority.valueOf(priority);
        } catch (IllegalArgumentException ex) {
            LogUtils.error("invalid event priority: {} (defaulting to MONITOR)", priority);
            eventPriority = EventPriority.MONITOR;
        }

        EventExecutor executor = (listener, event) -> {
            if (listener instanceof DeathListener && event instanceof PlayerDeathEvent deathEvent) {
                onDeath(deathEvent);
            }
        };

        AxGraves.getInstance().getServer().getPluginManager().registerEvent(
                PlayerDeathEvent.class,
                this,
                eventPriority,
                executor,
                AxGraves.getInstance(),
                true
        );
    }

    public void onDeath(PlayerDeathEvent event) {
        boolean debug = AxGraves.isDebugMode();
        Player player = event.getEntity();

        if (debug) LogUtils.debug("[{}] spawning grave", player.getName());
        if (disabledWorlds.contains(player.getWorld().getName())) {
            if (debug) LogUtils.debug("[{}] return: disabled world {}", player.getName(), player.getWorld().getName());
            return;
        }

        if (!player.hasPermission("axgraves.allowgraves")) {
            if (debug) LogUtils.debug("[{}] return: missing permission axgraves.allowgraves", player.getName());
            return;
        }

        if (player.getLastDamageCause() != null && blacklistedDeathCauses.contains(player.getLastDamageCause().getCause().name())) {
            if (debug) LogUtils.debug("[{}] return: blacklisted death cause {}", player.getName(), player.getLastDamageCause().getCause().name());
            return;
        }

        Location location = player.getLocation();
        location.add(0, -0.5, 0);
        if (debug) LogUtils.debug("[{}] location moved to {}", player.getName(), location.toString());

        final GravePreSpawnEvent gravePreSpawnEvent = new GravePreSpawnEvent(player, location);
        Bukkit.getPluginManager().callEvent(gravePreSpawnEvent);
        if (gravePreSpawnEvent.isCancelled()) {
            if (debug) LogUtils.debug("[{}] return: GravePreSpawnEvent cancelled", player.getName());
            return;
        }
        LocationUtils.DEATH_LOCATIONS.put(player.getUniqueId(), location);

        if (debug) {
            LogUtils.debug("[{}] storeItems: {} - getKeepInventory: {} - overrideKeepInventory: {}", player.getName(), storeItems, event.getKeepInventory(), overrideKeepInventory);
            LogUtils.debug("[{}] storeXP: {} - getKeepLevel: {} - overrideKeepLevel: {}", player.getName(), storeXP, event.getKeepLevel(), overrideKeepLevel);
        }

        List<ItemStack> drops = new ArrayList<>();
        if (storeItems) {
            boolean store = false;

            if (!event.getKeepInventory()) {
                store = true;
                if (softKeepInventory) {
                    int limit = (int) (event.getDrops().size() * softInventoryPercentage);
                    List<ItemStack> items = event.getDrops();
                    Collections.shuffle(items);
                    Iterator<ItemStack> iterator = items.iterator();

                    PlayerInventory inventory = player.getInventory();
                    List<ItemStack> armorContents = Arrays.asList(inventory.getArmorContents());
                    Random random = ThreadLocalRandom.current();
                    for (int i = 0; iterator.hasNext(); i++) {
                        ItemStack item = iterator.next();

                        if (i < limit) {
                            event.getItemsToKeep().add(item);
                            continue;
                        }

                        if (random.nextFloat() < softPityPercentage) {
                            if (softPityArmor && armorContents.contains(item)) {
                                event.getItemsToKeep().add(item);
                                continue;
                            }
                            if (softPityMainHand && inventory.getItemInMainHand().equals(item)) {
                                event.getItemsToKeep().add(item);
                                continue;
                            }
                            if (softPityOffHand && inventory.getItemInOffHand().equals(item)) {
                                event.getItemsToKeep().add(item);
                                continue;
                            }
                        }

                        drops.add(item);
                    }
                } else {
                    drops = event.getDrops();
                }
            } else if (overrideKeepInventory) {
                store = true;
                drops = Arrays.asList(player.getInventory().getContents());
                player.getInventory().clear();
            }

            if (store) {
                event.getDrops().clear();
            }
            if (debug) LogUtils.debug("[{}] store: {} - drops size: {}", player.getName(), store, drops.size());
        }

        int xp = 0;
        if (storeXP) {
            boolean store = false;
            if (!event.getKeepLevel()) {
                store = true;
            } else if (overrideKeepLevel) {
                store = true;
                player.setLevel(0);
                player.setTotalExperience(0);
            }

            if (store) {
                xp = Math.round(ExperienceUtils.getExp(player) * xpKeepPercentage);
                event.setDroppedExp(0);
            }
            if (debug) LogUtils.debug("[{}] store: {} - xp: {}", player.getName(), store, xp);
        }

        if (drops.isEmpty() && xp == 0) {
            if (debug) LogUtils.debug("[{}] return: drops empty and xp is 0", player.getName());
            return;
        }
        Grave grave = new Grave(location, player, drops, xp, System.currentTimeMillis());
        SpawnedGraves.addGrave(grave);
        if (debug) LogUtils.debug("[{}] created and added grave", player.getName());

        final GraveSpawnEvent graveSpawnEvent = new GraveSpawnEvent(player, grave);
        Bukkit.getPluginManager().callEvent(graveSpawnEvent);
    }

    @EventHandler
    public void onRespawn(@NotNull PlayerPostRespawnEvent event) {
        final Player player = event.getPlayer();

        if (CONFIG.getBoolean("respawn-title.enabled", false)) {
            final String title = CONFIG.getString("respawn-title.message", "");
            final long fadeIn = CONFIG.getLong("respawn-title.duration.fade-in", 0L);
            final long stay = CONFIG.getLong("respawn-title.duration.fade-in", 200L);
            final long fadeOut = CONFIG.getLong("respawn-title.duration.fade-in", 0L);
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
            }, CONFIG.getLong("respawn-title.delay", 40L));
        }

        if (CONFIG.getBoolean("respawn-compass.enabled", false)) {
            Location location = LocationUtils.DEATH_LOCATIONS.get(player.getUniqueId());
            if (location != null) {
                String rawDisplayName = CONFIG.getString("respawn-compass.display-name");
                List<String> rawLore = CONFIG.getStringList("respawn-compass.lore");
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