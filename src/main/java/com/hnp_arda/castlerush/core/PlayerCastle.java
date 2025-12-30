package com.hnp_arda.castlerush.core;

import com.hnp_arda.castlerush.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.hnp_arda.castlerush.Main.initWorldRules;
import static com.hnp_arda.castlerush.managers.GameManager.languageManager;

public class PlayerCastle {

    public final List<Marker> markers;
    private final World world;
    private final World nether;
    public Location castleStart;
    public Location castleEnd;


    public PlayerCastle(Player player, Main plugin) {
        this.markers = new ArrayList<>();

        world = loadWorld(player, plugin, false);
        nether = loadWorld(player, plugin, true);

        if (markers.isEmpty()) castleStart = castleEnd = world.getSpawnLocation().add(0.5, 1, 0.5);
    }

    private World loadWorld(Player player, Main plugin, boolean nether) {

        World w = loadExistingCastle(player, plugin, nether);
        if (w == null) w = createNewCastle(player, nether);

        if (w == null) {
            player.sendMessage(Component.text(languageManager.get("world.world_create_failed"), NamedTextColor.RED));
            throw new IllegalStateException("Could not create castle" + (nether ? "nether" : "world") + " for player " + player.getName() + "!");
        }

        initWorldRules(w);

        return w;
    }


    private World loadExistingCastle(Player player, Main plugin, boolean nether) {

        File playerDir = new File(new File(plugin.getDataFolder(), "Saves"), player.getName());
        if (!playerDir.exists()) return null;

        String name = "castle_rush_";
        if (nether) name += "nether_";
        name += player.getName();

        File savedWorld = new File(playerDir, name);

        if (!savedWorld.exists()) return null;

        File worldContainer = plugin.getServer().getWorldContainer();

        plugin.moveFolder(savedWorld, new File(worldContainer, name), "restore " + name);

        World w = new WorldCreator(name).createWorld();

        if (w != null && !nether) loadExistingCastleData(playerDir, plugin);

        return w;
    }

    public void loadExistingCastleData(File playerDir, Main plugin) {

        File saves = new File(playerDir, "castle.yml");
        if (!saves.exists()) {
            plugin.getLogger().info("Could not find castle.yml!");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(saves);

        castleStart = (Location) config.get("start");
        castleEnd = (Location) config.get("end");

        List<?> rawList = config.getList("markers", new ArrayList<>());

        if (!rawList.isEmpty())
            rawList.forEach((map) -> markers.add(Marker.constructFromSaveData(plugin.gameManager, (Map<?, ?>) map)));

        plugin.getLogger().info("Loaded existing Castle successfully!");
    }

    private World createNewCastle(Player player, boolean nether) {

        String name = "castle_rush_";
        if (nether) name += "nether_";
        name += player.getName();

        WorldCreator wc = new WorldCreator(name);

        if (nether) wc.environment(World.Environment.NETHER);
        else {
            wc.type(WorldType.FLAT);
            wc.environment(World.Environment.NORMAL);
            wc.generatorSettings("{\"layers\":[{\"block\":\"bedrock\",\"height\":1},{\"block\":\"stone\",\"height\":124},{\"block\":\"dirt\",\"height\":2},{\"block\":\"grass_block\",\"height\":1}],\"biome\":\"plains\"}");
            wc.generateStructures(false);
        }

        return wc.createWorld();
    }

    public void saveCastle(String playerName, Main plugin, boolean force) {

        saveMarkers(playerName, plugin);

        File savesDir = new File(plugin.getDataFolder(), "Saves");
        if (!savesDir.exists()) if (!savesDir.mkdirs()) {
            Bukkit.broadcast(Component.text(languageManager.get("game.save_error"), NamedTextColor.RED));
            return;
        }

        File playerDir = new File(savesDir, playerName);
        if (!playerDir.exists()) {
            if (!playerDir.mkdirs()) {
                Bukkit.broadcast(Component.text(languageManager.get("game.save_error_player", playerName), NamedTextColor.RED));
                return;
            }
        }

        boolean unloaded = Bukkit.unloadWorld(world, true) && Bukkit.unloadWorld(nether, true);
        if (!unloaded && !force) return;

        plugin.moveFolder(world.getWorldFolder(), new File(playerDir, world.getName()), "saving world " + playerName);
        plugin.moveFolder(nether.getWorldFolder(), new File(playerDir, nether.getName()), "saving nether " + playerName);

    }

    public void saveMarkers(String playerName, Main plugin) {
        File savesDir = new File(plugin.getDataFolder(), "Saves");
        if (!savesDir.exists()) if (!savesDir.mkdirs()) {
            Bukkit.broadcast(Component.text(languageManager.get("game.save_error"), NamedTextColor.RED));
            return;
        }

        File playerDir = new File(savesDir, playerName);
        if (!playerDir.exists()) {
            if (!playerDir.mkdirs()) {
                Bukkit.broadcast(Component.text(languageManager.get("game.save_error_player", playerName), NamedTextColor.RED));
                return;
            }
        }

        File saves = new File(playerDir, "castle.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(saves);

        config.set("start", castleStart);
        config.set("end", castleEnd);

        List<Map<String, String>> markersData = new ArrayList<>();
        markers.forEach(marker -> markersData.add(marker.getSaveData()));

        config.set("markers", markersData);

        try {
            config.save(saves);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player castle! (" + playerName + ")\n\n" + e.getMessage());
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

    public List<Marker> getZoneMarker(String data) {
        return markers.stream().filter(marker -> marker.getData().equals(data)).toList();
    }

    public Marker getMarker(Location loc) {
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
        return castleStart;
    }

    public void setStart(Location newStart) {
        world.setSpawnLocation(newStart.add(0.5, 1, 0.5));
        castleStart = newStart;
    }

    public Location getEnd() {
        return castleEnd;
    }

    public void setEnd(@Nullable Location newEnd) {
        if (newEnd == null) castleEnd = null;
        else castleEnd = newEnd.add(0.5, 0, 0.5);
    }
}
