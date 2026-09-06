package com.artillexstudios.axgraves.grave;

import com.artillexstudios.axgraves.AxGraves;
import com.artillexstudios.axgraves.utils.LimitUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.artillexstudios.axgraves.AxGraves.CONFIG;

public class SpawnedGraves {
    private static final ConcurrentLinkedQueue<Grave> graves = new ConcurrentLinkedQueue<>();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void addGrave(Grave grave) {
        Player player = grave.getPlayer().getPlayer();
        int graveLimit = player == null ? CONFIG.getInt("grave-limit", -1) : LimitUtils.getGraveLimit(player);

        if (graveLimit != -1) {
            int num = 0;
            Grave oldest = grave;

            for (Grave grave2 : graves) {
                if (!grave2.getPlayer().equals(grave.getPlayer())) continue;
                if (oldest.getSpawned() > grave2.getSpawned()) oldest = grave2;
                num++;
            }

            if (num >= graveLimit) oldest.remove();
        }

        graves.add(grave);
    }

    public static void removeGrave(Grave grave) {
        graves.remove(grave);
    }

    public static ConcurrentLinkedQueue<Grave> getGraves() {
        return graves;
    }

    public static void saveToFile() {
        List<SavedGrave> savedGraves = new ArrayList<>();
        for (Grave grave : graves) {
            SavedGrave savedGrave = SavedGrave.create(grave);
            savedGraves.add(savedGrave);
        }

        File file = new File(AxGraves.getInstance().getDataFolder(), "data.json");
        try (FileWriter fw = new FileWriter(file)) {
            gson.toJson(savedGraves, fw);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void loadFromFile() {
        SavedGrave[] savedGraves;
        File file = new File(AxGraves.getInstance().getDataFolder(), "data.json");
        if (!file.exists()) return;
        try (FileReader fw = new FileReader(file)) {
            savedGraves = gson.fromJson(fw, SavedGrave[].class);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        file.delete();
        if (savedGraves == null) return;

        for (SavedGrave savedGrave : savedGraves) {
            Grave grave = savedGrave.load();
            if (grave == null) continue;
            addGrave(grave);
        }
    }
}
