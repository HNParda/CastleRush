package com.hnp_arda.castlerush.core;

import com.hnp_arda.castlerush.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.hnp_arda.castlerush.core.MarkerSaveData.getMarkerSaveData;

public class PlayerCastle {

    public final List<Marker> markers;
    private final World world;
    private final World nether;
    public Location castleStart;
    public Location castleEnd;


    public PlayerCastle(World world, World nether) {
        this.world = world;
        this.nether = nether;
        this.markers = new ArrayList<>();
        castleStart = castleEnd = world.getSpawnLocation().add(0.5, 1, 0.5);
    }

    public void loadCastle(UUID uuid, Main plugin) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        File savesDir = new File(plugin.getDataFolder(), "Castle Saves");
        if (!savesDir.exists()) return;

        File saves = new File(savesDir, player.getName() + "_castle.yml");
        if (!saves.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(saves);

        castleStart = (Location) config.get("start");
        castleEnd = (Location) config.get("end");

        List<?> rawList = config.getList("markers");
        if (rawList == null) return;

        List<MarkerSaveData> markersData = new ArrayList<>();

        for (Object o : rawList)
            if (o instanceof MarkerSaveData m)
                markersData.add(m);

        markers.addAll(markersData.stream().map((markerSaveData) -> markerSaveData.getMarker(plugin.gameManager)).toList());
    }

    public void saveCastle(UUID uuid, Plugin plugin) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        File savesDir = new File(plugin.getDataFolder(), "Castle Saves");
        if (!savesDir.exists()) savesDir.mkdir();

        File saves = new File(savesDir, player.getName() + "_castle.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(saves);

        config.set("start", castleStart);
        config.set("end", castleEnd);

        List<MarkerSaveData> markersData = new ArrayList<>();
        markers.forEach(marker -> markersData.add(getMarkerSaveData(marker)));

        config.set("markers", markersData);

        try {
            config.save(saves);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player castle! (" + player.getName() + ")\n\n" + e.getMessage());
        }
    }

    public void removeMarker(Marker removeMarker) {
        markers.remove(removeMarker);
    }

    public void addMarker(Marker addMarker) {
        markers.add(addMarker);
    }

    public List<Marker> getMarkers(String typeId) {
        return markers.stream().filter(marker -> marker.getTypeId().equalsIgnoreCase(typeId)).toList();
    }

    public List<Marker> getAdvancedMarkers(String advancedToolData) {
        return markers.stream().filter(marker -> marker.getAdvancedToolData().equals(advancedToolData)).toList();
    }

    public Marker getLocation(Location loc) {
        return markers.stream().filter(marker -> marker.getLocation().equals(loc)).findFirst().orElse(null);
    }

    public World getCasleWorld() {
        return world;
    }

    public World getCastleNether() {
        return nether;
    }

    public List<Marker> getMarkers() {
        return markers;
    }

    public Location getStart() {
        if (castleStart == null) return world.getSpawnLocation();
        else return castleStart;
    }

    public void setStart(Location newStart) {
        if (newStart != null) world.setSpawnLocation(newStart);
        castleStart = newStart;
    }

    public Location getEnd() {
        return castleEnd;
    }

    public void setEnd(Location newEnd) {
        castleEnd = newEnd;
    }
}
