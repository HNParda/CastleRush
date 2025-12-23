package com.hnp_arda.castlerush;

import com.hnp_arda.castlerush.listeners.SpawnListener;
import com.hnp_arda.castlerush.managers.CommandManager;
import com.hnp_arda.castlerush.managers.GameManager;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.plugin.java.JavaPlugin;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class Main extends JavaPlugin {

    public Sign timeSign;
    public Location subTimeBtnLoc;
    public Location addTimeBtnLoc;
    public Location buildBtnLoc;
    public Location raceBtnLoc;
    public Location resetBtnLoc;

    public GameManager gameManager;
    public World spawnIsland;

    @Override
    public void onEnable() {
        getLogger().info("CaslteRush Plugin loaded!");

        createSpawnWorld();

        gameManager = new GameManager(this);

        Objects.requireNonNull(getCommand("castlerush")).setExecutor(new CommandManager(gameManager));

        SpawnListener spawnListener = new SpawnListener(this, gameManager, spawnIsland);
        RaceListener raceListener = new RaceListener(gameManager);

        getServer().getPluginManager().registerEvents(spawnListener, this);
        getServer().getPluginManager().registerEvents(raceListener, this);

        setupTimeControls();
        setupGameControls();

        getLogger().info("CaslteRush Plugin successfully enabled!");
    }

    private void createSpawnWorld() {
        WorldCreator wc = new WorldCreator("castle_rush_spawn");

        File worldFolder = new File(getServer().getWorldContainer(), "castle_rush_spawn");
        if (worldFolder.exists()) {
            spawnIsland = wc.createWorld();
            getLogger().info("CaslteRush Island found and loaded.");
        } else {
            getLogger().info("CastleRush Island not found. Creating...");

            wc.generator(new SpawnGenerator());
            wc.environment(World.Environment.NORMAL);
            wc.type(WorldType.NORMAL);
            wc.generateStructures(false);
            spawnIsland = wc.createWorld();

            if (spawnIsland == null) {
                getLogger().severe("Couldnt create new Spawn Island World!");
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

            getLogger().info("CaslteRush Island created!");
        }
        // spawnIsland.setTime(Time);

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

    public void updateTime(int minutes) {
        int newTimeSeconds = Math.max(600, gameManager.getBuildTimeSeconds() + 60 * minutes);
        gameManager.updateBuildTime(newTimeSeconds);
        updateTimeSign(newTimeSeconds / 60);
    }

    public void handleBtn(Location loc) {
        if (loc.equals(subTimeBtnLoc)) updateTime(-5);

        else if (loc.equals(addTimeBtnLoc)) updateTime(+5);

        else if (loc.equals(buildBtnLoc)) gameManager.startBuild();

        else if (loc.equals(raceBtnLoc)) gameManager.startRace();

        else if (loc.equals(resetBtnLoc)) gameManager.resetGame();

    }

    public void updateTimeSign(int minutes) {

        Bukkit.getScheduler().runTask(gameManager.getPlugin(), () -> {
            SignSide side = timeSign.getSide(Side.FRONT);

            side.line(0, Component.empty());
            side.line(1, Component.text("Build Time"));
            side.line(2, Component.text(minutes + " Min"));
            side.line(3, Component.empty());

            timeSign.update(true, false);
        });
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

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.cleanup();
        }
        try {
            File languageDir = new File(getDataFolder(), "languages");
            FileUtils.deleteDirectory(languageDir);
        } catch (IOException ignored) {
        }
        getLogger().info("CaslteRush Plugin deactivated! ");
    }

}
