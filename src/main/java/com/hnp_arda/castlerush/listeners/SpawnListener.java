package com.hnp_arda.castlerush.listeners;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.hnp_arda.castlerush.Main;
import com.hnp_arda.castlerush.managers.GameManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.*;

import static com.hnp_arda.castlerush.managers.GameManager.GameState.WAITING;

public class SpawnListener implements Listener {

    private final Main plugin;
    private final GameManager gameManager;
    private final World spawnIsland;

    public SpawnListener(Main plugin, GameManager gameManager, World spawnIsland) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.spawnIsland = spawnIsland;
    }

    @EventHandler
    public void onTimeButtonClick(PlayerInteractEvent event) {
        if (spawnIsland == null) return;
        if (event.getClickedBlock() == null) return;
        if (!event.getClickedBlock().getWorld().equals(spawnIsland)) return;
        if (gameManager.getGameState() != WAITING) return;
        if (event.getAction().isLeftClick()) return;

        Material type = event.getClickedBlock().getType();
        if (type == Material.PALE_OAK_WALL_SIGN) event.setCancelled(true);
        if (type != Material.PALE_OAK_BUTTON) return;

        Location loc = event.getClickedBlock().getLocation();

        plugin.handleBtn(loc);

        event.setCancelled(true);
    }

    @EventHandler
    public void onBreakBlock(BlockBreakEvent event) {
        if (spawnIsland == null) return;
        if (!event.getBlock().getWorld().equals(spawnIsland)) return;
        if (gameManager.getGameState() != WAITING) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent event) {
        if (spawnIsland == null) return;
        if (!event.getBlock().getWorld().equals(spawnIsland)) return;
        if (gameManager.getGameState() != WAITING) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlaceEntity(EntityPlaceEvent event) {
        if (spawnIsland == null) return;
        if (!event.getBlock().getWorld().equals(spawnIsland)) return;
        if (gameManager.getGameState() != WAITING) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (spawnIsland == null) return;
        if (!event.getBlock().getWorld().equals(spawnIsland)) return;
        if (gameManager.getGameState() != WAITING) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (spawnIsland == null) return;
        if (!event.getBlock().getWorld().equals(spawnIsland)) return;
        if (gameManager.getGameState() != WAITING) return;
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
            plugin.getLogger().info("CaslteRush JOINED " + player.getName());
            player.teleport(spawnIsland.getSpawnLocation().add(.5, 0, .5));
            player.setRespawnLocation(spawnIsland.getSpawnLocation().add(.5, 0, .5));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (spawnIsland == null) return;
        if (gameManager.getGameState() != WAITING) return;
        if (event.getPlayer().getLocation().getWorld() != spawnIsland) return;
        event.getPlayer().setAllowFlight(true);
        event.setRespawnLocation(spawnIsland.getSpawnLocation().add(.5, 0, .5));
    }

    @EventHandler
    public void onPostRespawn(PlayerPostRespawnEvent event) {
        if (spawnIsland == null) return;
        if (gameManager.getGameState() != WAITING) return;
        if (event.getPlayer().getLocation().getWorld() != spawnIsland) return;
        event.getPlayer().setAllowFlight(true);
    }

}
