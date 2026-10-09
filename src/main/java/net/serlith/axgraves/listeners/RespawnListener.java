package net.serlith.axgraves.listeners;

import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axgraves.AxGraves;
import com.artillexstudios.axgraves.grave.Grave;
import com.artillexstudios.axgraves.grave.SpawnedGraves;
import com.artillexstudios.axgraves.utils.LocationUtils;
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.title.Title;
import net.serlith.axgraves.utils.KeyUtils;
import net.serlith.axgraves.utils.SerlithUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NullMarked;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.artillexstudios.axgraves.AxGraves.CONFIG;

@NullMarked
public class RespawnListener implements Listener {

    private static boolean RESPAWN_TITLE_ENABLED;
    private static String RESPAWN_TITLE_MESSAGE = "";
    private static long RESPAWN_TITLE_DURATION_FADE_IN;
    private static long RESPAWN_TITLE_DURATION_STAY;
    private static long RESPAWN_TITLE_DURATION_FADE_OUT;
    private static long RESPAWN_TITLE_DELAY;

    private static boolean RESPAWN_COMPASS_ENABLED;
    private static String RESPAWN_COMPASS_DISPLAY_NAME = "";
    private static List<String> RESPAWN_COMPASS_LORE = List.of();

    public static void reload() {
        RESPAWN_TITLE_ENABLED = CONFIG.getBoolean("respawn-title.enabled", false);
        RESPAWN_TITLE_MESSAGE = CONFIG.getString("respawn-title.message", "");
        RESPAWN_TITLE_DURATION_FADE_IN = CONFIG.getLong("respawn-title.duration.fade-in", 0L);
        RESPAWN_TITLE_DURATION_STAY = CONFIG.getLong("respawn-title.duration.stay", 200L);
        RESPAWN_TITLE_DURATION_FADE_OUT = CONFIG.getLong("respawn-title.duration.fade-out", 0L);
        RESPAWN_TITLE_DELAY = CONFIG.getLong("respawn-title.delay", 2L);

        RESPAWN_COMPASS_ENABLED = CONFIG.getBoolean("respawn-compass.enabled", false);
        RESPAWN_COMPASS_DISPLAY_NAME = CONFIG.getString("respawn-compass.display-name", "");
        RESPAWN_COMPASS_LORE = CONFIG.getStringList("respawn-compass.lore", List.of());
    }

    public RespawnListener() {
        reload();
        AxGraves.getInstance().getServer().getPluginManager().registerEvents(this, AxGraves.getInstance());
    }

    @EventHandler
    public void onRespawn(final PlayerPostRespawnEvent event) {
        final Player player = event.getPlayer();

        if (RESPAWN_TITLE_ENABLED) {
            final String title = RESPAWN_TITLE_MESSAGE;
            final long fadeIn = RESPAWN_TITLE_DURATION_FADE_IN;
            final long stay = RESPAWN_TITLE_DURATION_STAY;
            final long fadeOut = RESPAWN_TITLE_DURATION_FADE_OUT;
            Bukkit.getAsyncScheduler().runDelayed(AxGraves.getInstance(), ignore -> {
                Location location = SerlithUtils.DEATH_LOCATIONS.get(player.getUniqueId());
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
            }, RESPAWN_TITLE_DELAY, TimeUnit.SECONDS);
        }

        if (RESPAWN_COMPASS_ENABLED) {
            Location location = SerlithUtils.DEATH_LOCATIONS.get(player.getUniqueId());
            if (location != null) {
                Scheduler.get().runAsync(() -> {
                    ItemStack compass = this.generateDeathCompass(player, location);
                    player.getScheduler().run(AxGraves.getInstance(), task -> player.getInventory().addItem(compass), null);
                });
            }
        }

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        final Block block = event.getClickedBlock();
        if (block == null) return;
        if (block.getType() == Material.LODESTONE) { // Don't re-use compasses
            if (SerlithUtils.isRespawnCompass(event.getItem())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Scheduler.get().runAsync(() -> {
            List<ItemStack> compasses = new ArrayList<>();
            for (Grave grave : SpawnedGraves.getGraves()) {
                compasses.add(this.generateDeathCompass(player, grave.getLocation()));
            }
            player.getScheduler().run(AxGraves.getInstance(), task -> player.getInventory().addItem(compasses.toArray(ItemStack[]::new)), null);
        });
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        SerlithUtils.DEATH_LOCATIONS.remove(event.getPlayer().getUniqueId());
        for (ItemStack item : event.getPlayer().getInventory()) {
            if (!SerlithUtils.isRespawnCompass(item)) {
                continue;
            }
            item.setAmount(0);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (!SerlithUtils.isRespawnCompass(event.getItem())) {
            return;
        }

        if (event.getSource() instanceof PlayerInventory inventory
                && inventory.getHolder() instanceof HumanEntity human
                && !human.hasPermission("axgraves.compass.move.bypass")
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity human = event.getWhoClicked();
        ItemStack cursor = event.getCursor();
        if (event.getClickedInventory() == event.getInventory() && SerlithUtils.isRespawnCompass(cursor) && !human.hasPermission("axgraves.compass.move.bypass")) {
            event.setCancelled(true);
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (event.getClick() == ClickType.SHIFT_LEFT && SerlithUtils.isRespawnCompass(clicked) && !human.hasPermission("axgraves.compass.move.bypass")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        ItemStack item = event.getCursor();
        if (!SerlithUtils.isRespawnCompass(item)) {
            return;
        }
        HumanEntity human = event.getWhoClicked();
        if (human.hasPermission("axgraves.compass.move.bypass")) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!SerlithUtils.isRespawnCompass(event.getItemDrop().getItemStack())) {
            return;
        }

        if (!event.getPlayer().hasPermission("axgraves.compass.move.bypass")) {
            event.getItemDrop().remove();
        }
    }

    private ItemStack generateDeathCompass(Player player, Location location) {
        String rawDisplayName = RESPAWN_COMPASS_DISPLAY_NAME;
        List<String> rawLore = RESPAWN_COMPASS_LORE;
        World world = location.getWorld();
        String worldName = LocationUtils.getWorldName(world);
        ItemStack compass = ItemStack.of(Material.COMPASS, 1);
        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        meta.setLodestone(location.clone());
        meta.setLodestoneTracked(false);
        meta.displayName(MiniMessage.miniMessage().deserialize(rawDisplayName).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        meta.lore(rawLore.stream().map(s -> MiniMessage.miniMessage().deserialize(s,
                        Placeholder.unparsed("player", player.getName()),
                        Placeholder.component("face", Component.object(ObjectContents.playerHead(player.getName()))),
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
        return compass;
    }

}
