package com.hnp_arda.castlerush.managers;

import com.hnp_arda.castlerush.Main;
import com.hnp_arda.castlerush.core.PlayerCastle;
import com.hnp_arda.castlerush.tools.ToolsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class GameManager {

    public static LanguageManager languageManager;
    private final Main plugin;
    private final ToolsManager toolsManager;
    private final RaceManager raceManager;
    private final ScoreboardManager scoreboardManager;

    private final Map<UUID, PlayerCastle> playerCastles;
    private final Set<UUID> participants;
    private GameState gameState;
    private int buildTimeSeconds;
    private int currentBuildTime;
    private BukkitRunnable buildTimer;
    private boolean pvpEnabled;

    public GameManager(Main plugin) {
        this.plugin = plugin;

        languageManager = new LanguageManager(plugin);
        this.raceManager = new RaceManager(this);
        this.toolsManager = new ToolsManager(this);
        this.scoreboardManager = new ScoreboardManager(plugin, this);

        this.playerCastles = new HashMap<>();
        this.participants = new HashSet<>();

        this.gameState = GameState.WAITING;
        this.buildTimeSeconds = 600;
        this.pvpEnabled = false;
    }

    public static LanguageManager getLanguageManager() {
        return languageManager;
    }

    public void setPvpEnabled(boolean enabled) {
        this.pvpEnabled = enabled;
    }

    private void applyPvpRule(boolean enabled) {
        for (PlayerCastle playerCastle : playerCastles.values()) {
            playerCastle.getCasleWorld().setGameRule(GameRule.PVP, enabled);
            playerCastle.getCastleNether().setGameRule(GameRule.PVP, enabled);
        }
    }

    public void setBuildTime(int seconds) {
        this.buildTimeSeconds = seconds;
    }

    public void updateBuildTime(int newTimeSeconds) {
        this.buildTimeSeconds = newTimeSeconds;
        Bukkit.broadcast(Component.text(GameManager.languageManager.get("command.time.set", newTimeSeconds / 60), NamedTextColor.GREEN));
    }

    public void addBuildTime(int seconds) {
        this.buildTimeSeconds += seconds;
        if (buildTimer != null) {
            currentBuildTime += seconds;
        }
    }

    public void startBuild(boolean forceStart) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        if (players.size() < 2 && !forceStart) {
            Bukkit.broadcast(Component.text(languageManager.get("command.start.not_enough_players"), NamedTextColor.RED));
            return;
        }

        String separator = "==============================";
        Bukkit.broadcast(Component.text(separator, NamedTextColor.GOLD));
        Bukkit.broadcast(Component.text(languageManager.get("command.start.header"), NamedTextColor.GREEN));
        Bukkit.broadcast(Component.text(languageManager.get("command.start.players", players.size()), NamedTextColor.YELLOW));
        Bukkit.broadcast(Component.text(languageManager.get("command.start.time", (getBuildTimeSeconds() / 60)), NamedTextColor.YELLOW));
        Bukkit.broadcast(Component.text(separator, NamedTextColor.GOLD));

        participants.clear();
        playerCastles.clear();

        gameState = GameState.BUILDING;
        currentBuildTime = buildTimeSeconds;

        for (Player player : players) {
            participants.add(player.getUniqueId());

            WorldCreator wc = new WorldCreator("castle_rush_" + player.getName());
            wc.type(WorldType.FLAT);
            wc.environment(World.Environment.NORMAL);
            wc.generatorSettings("{\"layers\":[{\"block\":\"bedrock\",\"height\":1},{\"block\":\"stone\",\"height\":124},{\"block\":\"dirt\",\"height\":2},{\"block\":\"grass_block\",\"height\":1}],\"biome\":\"plains\"}");
            wc.generateStructures(false);


            WorldCreator nwc = new WorldCreator("castle_rush_nether_" + player.getName());
            nwc.environment(World.Environment.NETHER);

            World nether = nwc.createWorld();
            World world = wc.createWorld();

            if (world == null || nether == null) {
                player.sendMessage(Component.text(languageManager.get("error.world_create_failed"), NamedTextColor.RED));
                continue;
            }


            getPlugin().initWorldRules(world);
            getPlugin().initWorldRules(nether);

            PlayerCastle playerCastle = new PlayerCastle(world, nether);
            playerCastles.put(player.getUniqueId(), playerCastle);

            player.teleport(world.getSpawnLocation());
            player.setGameMode(GameMode.CREATIVE);
            giveGoldTools(player);

            if (scoreboardManager != null) {
                scoreboardManager.createScoreboard(player);
            }

        }

        if (scoreboardManager != null) {
            scoreboardManager.startAutoUpdate();
        }

        loadCastles();

        startBuildTimer();
    }

    private void giveGoldTools(Player player) {
        player.getInventory().clear();
        getToolsManager().getTools().forEach(tool -> tool.giveToPlayer(player));
    }

    private void startBuildTimer() {
        buildTimer = new BukkitRunnable() {
            @Override
            public void run() {
                currentBuildTime--;
                if (currentBuildTime <= 0) {
                    startRace();
                    cancel();
                } else if (currentBuildTime % 60 == 0) {
                    broadcastToParticipants(Component.text(languageManager.get("game.build_time_remaining", (currentBuildTime / 60)), NamedTextColor.YELLOW));
                } else if (currentBuildTime <= 10) {
                    broadcastToParticipants(Component.text(String.valueOf(currentBuildTime), NamedTextColor.RED));
                }
            }
        };
        buildTimer.runTaskTimer(plugin, 20L, 20L);
    }

    public void startRace() {

        saveCastles();

        //IMPLEMENT CHECK IF PLAYERS ARE SAME AND CASTLE WORLDS EXYISTING
        if (buildTimer != null) buildTimer.cancel();
        if (scoreboardManager != null) scoreboardManager.stopAutoUpdate();

        Bukkit.broadcast(Component.text(languageManager.get("command.start.race_begin"), NamedTextColor.GREEN));

        gameState = GameState.RACING;
        broadcastToParticipants(Component.text(languageManager.get("game.build_phase_end"), NamedTextColor.GREEN));

        if (scoreboardManager != null) scoreboardManager.updateAllScoreboards();

        playerCastles.forEach((uuid, playerCastle) -> playerCastle.getMarkers().forEach(markerData -> Objects.requireNonNull(Bukkit.getPlayer(uuid)).sendBlockChange(markerData.getLocation(), markerData.getOriginalMaterial().createBlockData())));

        raceManager.startRace(participants);

        if (scoreboardManager != null) scoreboardManager.startAutoUpdate();

        broadcastToParticipants(Component.text(languageManager.get("game.race_start"), NamedTextColor.GOLD));
        applyPvpRule(pvpEnabled);
    }

    public void end() {
        saveCastles();
        if (buildTimer != null) buildTimer.cancel();
        if (scoreboardManager != null) {
            scoreboardManager.stopAutoUpdate();
        }
        gameState = GameState.WAITING;
    }

    private void saveCastles() {
        playerCastles.forEach((uuid, playerCastle) -> playerCastle.saveCastle(uuid, getPlugin()));
    }

    protected void loadCastles() {
        if (!checkPlayers()) return;
        playerCastles.forEach((uuid, castle) ->
                castle.loadCastle(uuid, plugin));
    }

    private boolean checkPlayers() {
        return true;
    }


    public void cleanup() {

        if (scoreboardManager != null) {
            scoreboardManager.stopAutoUpdate();
            scoreboardManager.removeAllScoreboards();
        }

        if (buildTimer != null) buildTimer.cancel();
        if (raceManager != null) raceManager.cleanup();
    }

    public void resetGame() {
        if (buildTimer != null) buildTimer.cancel();
        if (scoreboardManager != null) {
            scoreboardManager.stopAutoUpdate();
            scoreboardManager.removeAllScoreboards();
        }

        File castlesData = new File(plugin.getDataFolder(), "Castles Data");

        try {
            FileUtils.deleteDirectory(castlesData);
        } catch (IOException ignored) {
        }

        World hub = Bukkit.getWorld("castle_rush_spawn");
        if (hub == null) hub = Bukkit.getWorlds().getFirst();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(hub.getSpawnLocation());
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.setExp(0);
            player.setLevel(0);
            player.setAllowFlight(true);
        }

        List<World> worldsToDelete = new ArrayList<>();
        for (World world : Bukkit.getWorlds())
            if (world.getName().startsWith("castle_rush_")) worldsToDelete.add(world);

        if (worldsToDelete.isEmpty()) {
            File[] folders = getPlugin().getServer().getWorldContainer().listFiles();
            if (folders != null) for (File folder : folders) {
                if (folder.isDirectory() && folder.getName().startsWith("castle_rush_")) {
                    try {
                        FileUtils.deleteDirectory(new File(folder.getAbsolutePath()));
                    } catch (IOException e) {
                        getPlugin().getLogger().severe(e.getMessage());
                    }
                }
            }
        } else for (World world : worldsToDelete) {
            String name = world.getName();
            Bukkit.unloadWorld(world, false);
            File folder = world.getWorldFolder();
            deleteWorld(folder);
            plugin.getLogger().info(name + " deleted");
        }

        gameState = GameState.WAITING;
        participants.clear();
        playerCastles.clear();
        currentBuildTime = 0;
        raceManager.cleanup();

        Bukkit.broadcast(Component.text(languageManager.get("game.reset"), NamedTextColor.RED));
    }

    private void deleteWorld(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            try {
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            deleteWorld(file);
                        } else {
                            FileUtils.deleteDirectory(file);
                        }
                    }
                }
                FileUtils.deleteDirectory(path);
            } catch (IOException e) {
                getPlugin().getLogger().warning("Error deleting world. " + e.getMessage());
            }
        }
    }

    public PlayerCastle getPlayerCastle(Player player) {
        return playerCastles.get(player.getUniqueId());
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public Main getPlugin() {
        return plugin;
    }

    private void broadcastToParticipants(Component message) {
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.sendMessage(message);
        }
    }

    public RaceManager getRaceManager() {
        return raceManager;
    }

    public GameState getGameState() {
        return gameState;
    }

    public int getBuildTimeSeconds() {
        return buildTimeSeconds;
    }

    public int getCurrentBuildTime() {
        return currentBuildTime;
    }

    public ToolsManager getToolsManager() {
        return toolsManager;
    }

    public enum GameState {
        WAITING, BUILDING, RACING
    }
}


