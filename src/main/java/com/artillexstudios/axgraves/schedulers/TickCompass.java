package com.artillexstudios.axgraves.schedulers;

import com.artillexstudios.axapi.utils.ActionBar;
import com.artillexstudios.axgraves.utils.Utils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.artillexstudios.axgraves.AxGraves.*;

public class TickCompass {
    private static ScheduledFuture<?> future = null;

    public static void start() {
        future = EXECUTOR.scheduleAtFixedRate(() -> {
            if (!CONFIG.getBoolean("respawn-compass.enabled", false)) {
                return;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (!Utils.isRespawnCompass(item)) {
                    item = player.getInventory().getItemInOffHand();
                    if (!Utils.isRespawnCompass(item)) {
                        continue;
                    }
                }

                CompassMeta meta = (CompassMeta) item.getItemMeta();
                Location location = meta.getLodestone();
                if (location == null) {
                    continue;
                }

                ActionBar.create(
                        MiniMessage.miniMessage().deserialize(MESSAGES.getString("respawn-compass.message"),
                                Placeholder.unparsed("distance", String.format("%.0f", location.distance(player.getLocation()))))
                ).send(player);
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }

    public static void stop() {
        if (future == null) return;
        future.cancel(true);
    }

}
