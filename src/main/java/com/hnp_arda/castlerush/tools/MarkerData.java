package com.hnp_arda.castlerush.tools;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class MarkerData {

    private final Location location;
    private final String typeId;
    private final String translationKey;
    private Material originalMaterial;
    private final Tool tool;
    private String advancedToolData;

    public MarkerData(Tool tool, Location location, Material originalMaterial, String typeId, String translationKey) {
        this.location = location;
        this.originalMaterial = originalMaterial;
        this.typeId = typeId;
        this.translationKey = translationKey;
        this.tool = tool;
    }

    //WIRD NOCH GEÄNDERT HARDCODED FSKadDKS
    public MarkerData(Tool tool, Location location, Material originalMaterial, String typeId, String translationKey, String advancedToolData) {
        this(tool, location, originalMaterial, typeId, translationKey);
        this.advancedToolData = advancedToolData;
    }

    public static String formatLocation(Location loc) {
        return String.format("X:%d Y:%d Z:%d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public Location getLocation() {
        return location;
    }

    public Material getOriginalMaterial() {
        return originalMaterial;
    }

    public void setOriginalMaterial(Material newMaterial) {
        originalMaterial = newMaterial;
    }

    public String getTypeId() {
        return typeId;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public Material getDisplayMaterial() {
        World world =  location.getWorld();
        Material originalMaterial = world.getBlockAt(getLocation()).getType();
        setOriginalMaterial(originalMaterial);
        return tool.getDisplayMaterial(world, this);
    }

    public String getAdvancedToolData() {
        return advancedToolData;
    }

    public void setAdvancedToolData(String advancedToolData) {
        this.advancedToolData = advancedToolData;
    }

    public void triggerMarkerEnter(Player player) {
        tool.triggerEnter(player, this);
    }

    public void triggerMarkerExit(Player player) {
        tool.triggerExit(player);
    }

    public boolean isAdvancedMarker() {
        return !getAdvancedToolData().isEmpty();
    }


}
