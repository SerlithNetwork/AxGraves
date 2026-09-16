package com.artillexstudios.axgraves.utils;

import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.scheduler.impl.SchedulerHandler;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SchedulerOverrideUtils {

    public static void override(JavaPlugin plugin) {
        try {
            Server server = Bukkit.getServer();
            Method method = server.getClass().getMethod("isParallelWorldTickingEnabled");
            boolean enabled = (Boolean) method.invoke(server);
            if (!enabled) {
                plugin.getSLF4JLogger().info("Parallel World Ticking supported but not enabled");
                return;
            }

            plugin.getSLF4JLogger().info("Enabling Parallel World Ticking support...");
            SchedulerHandler handler = Scheduler.scheduler;
            Field field = handler.getClass().getDeclaredField("scheduler");
            field.setAccessible(true);
            field.set(handler, (Scheduler)Class.forName("com.artillexstudios.axapi.scheduler.impl.FoliaScheduler").getDeclaredConstructor(JavaPlugin.class).newInstance(plugin));
            plugin.getSLF4JLogger().info("Parallel World Ticking support enabled!");
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
        } catch (InvocationTargetException | NoSuchFieldException | ClassNotFoundException | InstantiationException exception) {
            throw new IllegalStateException(exception);
        }
    }

}
