package com.hnp_arda.castlerush.core;

import com.hnp_arda.castlerush.managers.GameManager;
import com.hnp_arda.castlerush.tools.BaseTool;
import com.hnp_arda.castlerush.tools.BaseZoneTool;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class Marker {

    private final Location location;
    private final BaseTool tool;
    private Material originalMaterial;
    private String data;

    public Marker(BaseTool tool, Location location) {
        this(tool, location, tool.getTypeId());
    }

    public Marker(BaseTool tool, Location location, String data) {
        this.location = location;
        this.tool = tool;
        this.data = data;

        World world = location.getWorld();
        Material originalMaterial = world.getBlockAt(getLocation()).getType();
        setOriginalMaterial(originalMaterial);
    }

    public static String formatLocation(Location loc) {
        if (loc == null) return "NULL";
        return String.format("X:%d Y:%d Z:%d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public static Marker constructFromSaveData(GameManager gameManager, Map<?, ?> map) {
        World world = Bukkit.getWorld((String) map.get("world"));
        double x = Double.parseDouble((String) map.get("x"));
        double y = Double.parseDouble((String) map.get("y"));
        double z = Double.parseDouble((String) map.get("z"));
        Location location = new Location(world, x, y, z);
        String data = (String) map.get("data");
        BaseTool tool = gameManager.getToolsManager().getToolByTypeID(data.split(";")[0]);

        return new Marker(tool, location, data);
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
        return tool.getTypeId();
    }

    public String getTranslationKey() {
        return tool.getTranslationKey();
    }

    public Material getDisplayMaterial() {
        World world = location.getWorld();
        Material originalMaterial = world.getBlockAt(getLocation()).getType();
        setOriginalMaterial(originalMaterial);
        return tool.getDisplayMaterial(world, this);
    }

    public String getData() {
        return data;
    }

    public boolean isAir() {
        World world = location.getWorld();
        Material m = world.getBlockAt(getLocation()).getType();
        return m.isAir() || m == Material.AIR;
    }

    public boolean isZoneTool() {
        return tool instanceof BaseZoneTool;
    }

    public void triggerMarkerEnter(Player player) {
        tool.triggerEnter(player, this);
    }

    public void triggerMarkerExit(Player player) {
        tool.triggerExit(player);
    }

    public boolean isReplaceable() {
        return tool.isReplaceable();
    }

    public Map<String, String> getSaveData() {

        org.bukkit.Location location = getLocation();
        String locationWorld = location.getWorld().getName();
        String locationX = String.valueOf(location.getBlockX());
        String locationY = String.valueOf(location.getBlockY());
        String locationZ = String.valueOf(location.getBlockZ());
        String data = getData();

        Map<String, String> map = new HashMap<>();
        map.put("world", locationWorld);
        map.put("x", locationX);
        map.put("y", locationY);
        map.put("z", locationZ);
        map.put("data", data);
        return map;
    }

    public void setData(String data) {
        this.data = data;
    }
}
