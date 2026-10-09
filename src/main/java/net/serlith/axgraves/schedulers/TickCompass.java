package net.serlith.axgraves.schedulers;

import com.artillexstudios.axapi.utils.ActionBar;
import com.artillexstudios.axgraves.utils.Utils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.serlith.axgraves.utils.SerlithUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.artillexstudios.axgraves.AxGraves.*;

@NullMarked
public class TickCompass {
    private static @Nullable ScheduledFuture<?> future = null;

    public static void start() {
        future = EXECUTOR.scheduleAtFixedRate(() -> {
            if (!CONFIG.getBoolean("respawn-compass.enabled", false)) {
                return;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (!SerlithUtils.isRespawnCompass(item)) {
                    item = player.getInventory().getItemInOffHand();
                    if (!SerlithUtils.isRespawnCompass(item)) {
                        continue;
                    }
                }

                CompassMeta meta = (CompassMeta) item.getItemMeta();
                Location location = meta.getLodestone();
                if (location == null) {
                    continue;
                }

                Location playerLocation = player.getLocation();
                if (!Objects.equals(playerLocation.getWorld(), location.getWorld())) {
                    continue;
                }

                ActionBar.create(
                        MiniMessage.miniMessage().deserialize(LANG.getString("respawn-compass.message"),
                                Placeholder.unparsed("distance", String.format("%.0f", location.distance(playerLocation))))
                ).send(player);
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }

    public static void stop() {
        if (future == null) return;
        future.cancel(true);
    }

}
