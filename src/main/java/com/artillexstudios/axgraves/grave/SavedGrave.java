package com.artillexstudios.axgraves.grave;

import com.artillexstudios.axapi.serializers.Serializers;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

public class SavedGrave {
    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private String location;
    private String owner;
    private String texture;
    private String items;
    private int xp;
    private long date;

    public static SavedGrave create(Grave grave) {
        return new SavedGrave(
                Serializers.LOCATION.serialize(grave.getLocation()),
                grave.getPlayer().getUniqueId().toString(),
                grave.getTexture(),
                ENCODER.encodeToString(Serializers.ITEM_ARRAY.serialize(grave.getGui().getContents())),
                grave.getStoredXP(),
                grave.getSpawned()
        );
    }

    public SavedGrave() {
    }

    public SavedGrave(String location, String owner, String texture, String items, int xp, long date) {
        this.location = location;
        this.owner = owner;
        this.texture = texture;
        this.items = items;
        this.xp = xp;
        this.date = date;
    }

    @Nullable
    public Grave load() {
        Location loc = Serializers.LOCATION.deserialize(location);
        if (loc == null || loc.getWorld() == null) return null;
        return new Grave(
                loc,
                Bukkit.getOfflinePlayer(UUID.fromString(owner)),
                Arrays.asList(Serializers.ITEM_ARRAY.deserialize(DECODER.decode(items))),
                xp,
                date,
                texture
        );
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    public String getItems() {
        return items;
    }

    public void setItems(String items) {
        this.items = items;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }
}
