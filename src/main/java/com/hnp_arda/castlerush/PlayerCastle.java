package com.hnp_arda.castlerush;

import com.hnp_arda.castlerush.tools.MarkerData;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class PlayerCastle {

    private final World world;
    private final World nether;
    private final List<MarkerData> markers;
    private Location castleStart;
    private Location castleEnd;
    private Location deathzoneStart;
    private Location effectZoneStart;


    public PlayerCastle(World world, World nether) {
        this.world = world;
        this.nether = nether;
        this.markers = new ArrayList<>();
        this.deathzoneStart = null;
        this.effectZoneStart = null;
        castleStart = castleEnd = world.getSpawnLocation().add(0.5, 1, 0.5);
    }


    public void removeMarker(MarkerData removeMarker) {
        markers.remove(removeMarker);
    }

    public void addMarker(MarkerData addMarker) {
        markers.add(addMarker);
    }

    public List<MarkerData> getMarker(String typeId) {
        return markers.stream().filter(marker -> marker.getTypeId().equalsIgnoreCase(typeId)).toList();
    }

    public Location getDeathzoneStart() {
        return deathzoneStart;
    }

    public void setDeathzoneStart(Location location) {
        this.deathzoneStart = location;
    }

    public boolean hasDeathzoneStart() {
        return deathzoneStart != null;
    }

    public boolean isDeathzoneBlock(Location loc) {
        return markers.stream().anyMatch(marker -> marker.getTypeId().equalsIgnoreCase("deathzone") && marker.getLocation().equals(loc));
    }

    public Location getEffectZoneStart() {
        return effectZoneStart;
    }

    public void setEffectZoneStart(Location location) {
        this.effectZoneStart = location;
    }

    public boolean hasEffectZoneStart() {
        return effectZoneStart != null;
    }

    public boolean isEffectBlock(Location loc) {
        return markers.stream().anyMatch(marker -> marker.getTypeId().equalsIgnoreCase("effect") && marker.getLocation().equals(loc));
    }

    public MarkerData getLocation(Location loc) {
        return markers.stream().filter(marker -> marker.getLocation().equals(loc)).findFirst().orElse(null);
    }

    public World getCasleWorld() {
        return world;
    }

    public World getCastleNether() {
        return nether;
    }

    public List<MarkerData> getMarkers() {
        return markers;
    }

    public Location getStart() {
        if (castleStart == null) return world.getSpawnLocation();
        else return castleStart;
    }

    public void setStart(Location newStart) {
        world.setSpawnLocation(newStart);
        castleStart = newStart;
    }

    public Location getEnd() {
        return castleEnd;
    }

    public void setEnd(Location newEnd) {
        castleEnd = newEnd;
    }
}
