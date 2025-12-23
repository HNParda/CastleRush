package com.hnp_arda.castlerush.core;

import com.hnp_arda.castlerush.managers.GameManager;
import com.hnp_arda.castlerush.tools.BaseTool;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;


public class MarkerSaveData {

    protected final String locationWorld;
    protected final String locationX;
    protected final String locationY;
    protected final String locationZ;

    protected final String tool;

    protected final String advancedToolData;

    protected MarkerSaveData(String locationWorld, String locationX, String locationY, String locationZ, String tool, String advancedToolData) {
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


    protected Marker getMarker(GameManager gameManager) {
        World world = Bukkit.getWorld(this.locationWorld);
        double x = Double.parseDouble(this.locationX);
        double y = Double.parseDouble(this.locationY);
        double z = Double.parseDouble(this.locationZ);

        Location location = new Location(world, x, y, z);
        BaseTool tool = gameManager.getToolsManager().getToolByTypeID(this.tool);

        return new Marker(tool, location, advancedToolData);

    }

}
