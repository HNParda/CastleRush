package com.hnp_arda.castlerush.core;

import com.hnp_arda.castlerush.managers.GameManager;
import com.hnp_arda.castlerush.tools.BaseTool;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;


@SerializableAs("MarkerSaveData")
public class MarkerSaveData implements ConfigurationSerializable {

    public String locationWorld;
    public String locationX;
    public String locationY;
    public String locationZ;

    public String tool;

    public String advancedToolData;

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("world", locationWorld);
        map.put("x", locationX);
        map.put("y", locationY);
        map.put("z", locationZ);
        map.put("tool", tool);
        map.put("advanced", advancedToolData);
        return map;
    }

    public MarkerSaveData(String locationWorld, String locationX, String locationY, String locationZ, String tool, String advancedToolData) {
        this.locationWorld = locationWorld;
        this.locationX = locationX;
        this.locationY = locationY;
        this.locationZ = locationZ;
        this.tool = tool;
        this.advancedToolData = advancedToolData;
    }

    public static  MarkerSaveData getMarkerSaveData(Marker marker) {
        org.bukkit.Location location = marker.getLocation();
        String locationWorld = location.getWorld().getName();
        String locationX = String.valueOf(location.getBlockX());
        String locationY = String.valueOf(location.getBlockY());
        String locationZ = String.valueOf(location.getBlockZ());
        String tool = marker.getTool().getTypeId();
        String advancedToolData = marker.getAdvancedToolData();

        return new MarkerSaveData(locationWorld, locationX, locationY, locationZ, tool, advancedToolData);
    }

    @SuppressWarnings("unused")
    public static MarkerSaveData deserialize(Map<String, Object> map) {
        return new MarkerSaveData(
                (String) map.get("world"),
                (String) map.get("x"),
                (String) map.get("y"),
                (String) map.get("z"),
                (String) map.get("tool"),
                (String) map.get("advanced")
        );
    }

    public Marker getMarker(GameManager gameManager) {
        World world = Bukkit.getWorld(this.locationWorld);
        double x = Double.parseDouble(this.locationX);
        double y = Double.parseDouble(this.locationY);
        double z = Double.parseDouble(this.locationZ);

        Location location = new Location(world, x, y, z);
        BaseTool tool = gameManager.getToolsManager().getToolByTypeID(this.tool);

        return new Marker(tool, location, advancedToolData);

    }

}
