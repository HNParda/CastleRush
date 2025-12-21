package com.hnp_arda.castlerush;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class Main extends JavaPlugin implements Listener {

    public Sign timeSign;
    public Location subTimeBtnLoc;
    public Location addTimeBtnLoc;
    public Location buildBtnLoc;
    public Location raceBtnLoc;
    public Location resetBtnLoc;

    private GameManager gameManager;
    private World spawnIsland;

    @Override
    public void onEnable() {
        getLogger().info("CaslteRush Plugin wurde geladen!");

        gameManager = new GameManager(this);

        getServer().getPluginManager().registerEvents(this, this);

        Objects.requireNonNull(getCommand("castlerush")).setExecutor(new CommandManager(gameManager));

        createHubWorld();

        getLogger().info("CaslteRush Plugin wurde erfolgreich gestartet!");
    }

    private void createHubWorld() {
        WorldCreator wc = new WorldCreator("castle_rush_spawn");

        File worldFolder = new File(getServer().getWorldContainer(), "castle_rush_spawn");
        if (worldFolder.exists()) {
            spawnIsland = wc.createWorld();
            getLogger().info("CaslteRush Island gefunden und geladen.");
            setupTimeControls();
        } else {
            getLogger().info("CaslteRush Island nicht gefunden. Wird erstellt");

            wc.generator(new SpawnGenerator());
            wc.environment(World.Environment.NORMAL);
            wc.type(WorldType.NORMAL);
            wc.generateStructures(false);
            spawnIsland = wc.createWorld();

            if (spawnIsland == null) {
                getLogger().severe("Welt konnte nicht erstellt werden!");
                return;
            }

            spawnIsland.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
            spawnIsland.setGameRule(GameRule.KEEP_INVENTORY, true);
            spawnIsland.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            spawnIsland.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            spawnIsland.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            spawnIsland.setGameRule(GameRule.PVP, true);
            spawnIsland.setGameRule(GameRule.DISABLE_ELYTRA_MOVEMENT_CHECK, true);
            spawnIsland.setGameRule(GameRule.DISABLE_PLAYER_MOVEMENT_CHECK, true);
            spawnIsland.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
            spawnIsland.setSpawnLocation(8, 80, 8, -45F);

            getLogger().info("CaslteRush Plugin wurde gestartet!");
        }

        setupTimeControls();
        setupGameControls();
    }

    private void updateTime(int minutes) {
        int newTimeSeconds = Math.max(300, gameManager.getBuildTimeSeconds() + 60 * minutes);
        gameManager.updateBuildTime(newTimeSeconds);
        updateTimeSing(newTimeSeconds / 60);
    }

    public void updateTimeSing(int minutes) {

        Bukkit.getScheduler().runTask(gameManager.getPlugin(), () -> {
            var side = timeSign.getSide(Side.FRONT);

            side.line(0, Component.empty());
            side.line(1, Component.text("Build Time"));
            side.line(2, Component.text(minutes + " Min"));
            side.line(3, Component.empty());

            timeSign.update(true, false);
        });
    }

    private Sign createSign(String message, int x, int y, int z, BlockFace facing) {

        Location signLoc = new Location(spawnIsland, x, y, z);
        WallSign wallSign = (WallSign) Material.PALE_OAK_WALL_SIGN.createBlockData();
        wallSign.setFacing(facing);
        spawnIsland.setBlockData(signLoc, wallSign);

        Sign sign = (Sign) spawnIsland.getBlockAt(signLoc).getState();

        if (!message.isBlank()) {
            String[] lines = message.split("\n");
            sign.getSide(Side.FRONT).line(0, Component.text(lines[0]));
            sign.getSide(Side.FRONT).line(1, Component.text(lines[1]));
            sign.getSide(Side.FRONT).line(2, Component.text(lines[2]));
            sign.getSide(Side.FRONT).line(3, Component.text(lines[3]));
            sign.update(true, false);
        }

        return sign;
    }

    private void setupTimeControls() {
        if (spawnIsland == null) return;

        createSign(" \n-5\nMinutes\n ", 13, 82, 7, BlockFace.WEST);
        createSign(" \n+5\nMinutes\n ", 13, 82, 9, BlockFace.WEST);

        timeSign = createSign("", 13, 81, 8, BlockFace.WEST);
        updateTime(0);

        subTimeBtnLoc = new Location(spawnIsland, 13, 81, 7);
        Switch subTimeBtn = (Switch) Material.PALE_OAK_BUTTON.createBlockData();
        subTimeBtn.setFacing(BlockFace.WEST);
        spawnIsland.setBlockData(subTimeBtnLoc, subTimeBtn);


        addTimeBtnLoc = new Location(spawnIsland, 13, 81, 9);
        Switch addTimeBtn = (Switch) Material.PALE_OAK_BUTTON.createBlockData();
        addTimeBtn.setFacing(BlockFace.WEST);
        spawnIsland.setBlockData(addTimeBtnLoc, addTimeBtn);
    }

    private void setupGameControls() {
        if (spawnIsland == null) return;

        createSign(" \nStart\nBuilding\n ", 10, 82, 13, BlockFace.NORTH);

        buildBtnLoc = new Location(spawnIsland, 10, 81, 13);
        Switch buildBtn = (Switch) Material.PALE_OAK_BUTTON.createBlockData();
        buildBtn.setFacing(BlockFace.NORTH);
        spawnIsland.setBlockData(buildBtnLoc, buildBtn);


        createSign(" \nStart\nRacing\n ", 8, 82, 13, BlockFace.NORTH);

        raceBtnLoc = new Location(spawnIsland, 8, 81, 13);
        Switch raceBtn = (Switch) Material.PALE_OAK_BUTTON.createBlockData();
        raceBtn.setFacing(BlockFace.NORTH);
        spawnIsland.setBlockData(raceBtnLoc, raceBtn);


        createSign(" \nReset\nCastles\n ", 6, 82, 13, BlockFace.NORTH);

        resetBtnLoc = new Location(spawnIsland, 6, 81, 13);
        Switch resetBtn = (Switch) Material.PALE_OAK_BUTTON.createBlockData();
        resetBtn.setFacing(BlockFace.NORTH);
        spawnIsland.setBlockData(resetBtnLoc, resetBtn);
    }

    @EventHandler
    public void onTimeButtonClick(PlayerInteractEvent event) {
        if (spawnIsland == null) return;
        if (event.getClickedBlock() == null) return;
        if (!event.getClickedBlock().getWorld().equals(spawnIsland)) return;
        if (gameManager.getGameState() != GameManager.GameState.WAITING) return;
        if (event.getAction().isLeftClick()) return;

        Material type = event.getClickedBlock().getType();
        if (type == Material.PALE_OAK_WALL_SIGN) event.setCancelled(true);
        if (type != Material.PALE_OAK_BUTTON) return;

        Location loc = event.getClickedBlock().getLocation();

        if (loc.equals(subTimeBtnLoc))
            updateTime(-5);

        else if (loc.equals(addTimeBtnLoc))
            updateTime(+5);

        else if (loc.equals(buildBtnLoc))
            gameManager.startBuild();

        else if (loc.equals(raceBtnLoc))
            gameManager.startRace();

        else if (loc.equals(resetBtnLoc))
            gameManager.resetGame();
        else return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onBreakBlock(BlockBreakEvent event) {
        if (spawnIsland == null) return;
        if (!event.getBlock().getWorld().equals(spawnIsland)) return;
        if (gameManager.getGameState() != GameManager.GameState.WAITING) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent event) {
        if (spawnIsland == null) return;
        if (!event.getBlock().getWorld().equals(spawnIsland)) return;
        if (gameManager.getGameState() != GameManager.GameState.WAITING) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlaceEntity(EntityPlaceEvent event) {
        if (spawnIsland == null) return;
        if (!event.getBlock().getWorld().equals(spawnIsland)) return;
        if (gameManager.getGameState() != GameManager.GameState.WAITING) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (spawnIsland == null) return;
        if (!event.getBlock().getWorld().equals(spawnIsland)) return;
        if (gameManager.getGameState() != GameManager.GameState.WAITING) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (spawnIsland == null) return;
        if (!event.getBlock().getWorld().equals(spawnIsland)) return;
        if (gameManager.getGameState() != GameManager.GameState.WAITING) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (spawnIsland != null) {
            Player player = event.getPlayer();
            player.setAllowFlight(true);
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.setExp(0);
            player.setLevel(0);
            getLogger().info("CaslteRush JOINED " + player.getName());
            player.teleport(spawnIsland.getSpawnLocation().add(.5, 0, .5));
            player.setRespawnLocation(spawnIsland.getSpawnLocation().add(.5, 0, .5));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (spawnIsland == null) return;
        if (event.getPlayer().getLocation().getWorld() != spawnIsland) return;
        event.getPlayer().setAllowFlight(true);
        event.setRespawnLocation(spawnIsland.getSpawnLocation().add(.5, 0, .5));
    }

    @EventHandler
    public void onPostRespawn(PlayerPostRespawnEvent event) {
        if (spawnIsland == null) return;
        if (event.getPlayer().getLocation().getWorld() != spawnIsland) return;
        event.getPlayer().setAllowFlight(true);
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        String fromWorldName = from.getWorld().getName();
        if (!fromWorldName.startsWith("castle_rush_")) return;

        if (fromWorldName.contains("nether")) {

            String worldName = fromWorldName.replace("_nether", "");
            World world = Bukkit.getWorld(worldName);

            event.setCreationRadius(0);
            event.setSearchRadius(16);

            Location targetLoc = new Location(world, to.getX(), to.getY(), to.getZ());

            assert world != null;
            int highestY = world.getHighestBlockYAt(targetLoc);

            event.setTo(targetLoc);

            if (targetLoc.getBlockY() < highestY - 5) {
                Location surfaceLoc = new Location(world, to.getX(), highestY + 1, to.getZ());
                event.setTo(surfaceLoc);
                getLogger().info("Portal korrigiert von Y:" + to.getBlockY() + " zu Y:" + (highestY + 1));
            }

        } else {
            String netherName = fromWorldName.replace("castle_rush_", "castle_rush_nether_");
            World nether = Bukkit.getWorld(netherName);
            event.setCreationRadius(0);
            event.setSearchRadius(1);
            event.setTo(new Location(nether, to.getX(), to.getY(), to.getZ()));
        }
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.cleanup();
        }
        if (getDataFolder().exists()) {
            try {
                FileUtils.deleteDirectory(getDataFolder());
            } catch (IOException ignored) {
            }
        }
        getLogger().info("CaslteRush Plugin wurde deaktiviert! ");
    }

}
