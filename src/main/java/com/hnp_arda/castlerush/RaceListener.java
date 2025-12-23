package com.hnp_arda.castlerush;

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import com.hnp_arda.castlerush.managers.GameManager;
import com.hnp_arda.castlerush.managers.GameManager.GameState;
import com.hnp_arda.castlerush.managers.RaceManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;

public record RaceListener(GameManager gameManager) implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (gameManager.getGameState() != GameState.RACING) return;
        if (!event.hasChangedBlock()) return;

        Player player = event.getPlayer();
        RaceManager raceManager = gameManager.getRaceManager();

        raceManager.checkTrigger(player, event.getFrom(), event.getTo());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (gameManager.getGameState() != GameState.RACING) return;

        Player player = event.getEntity();

        event.setCancelled(true);

        RaceManager.RaceProgress progress = gameManager.getRaceManager().getPlayerProgress().get(player.getUniqueId());
        if (progress == null || progress.isFinished()) return;

        RaceManager raceManager = gameManager.getRaceManager();
        raceManager.handlePlayerDeath(player);

    }


    @EventHandler
    public void onSpectate(PlayerStartSpectatingEntityEvent event) {

        if (!(event.getNewSpectatorTarget() instanceof Player target)) return;

        RaceManager raceManager = gameManager.getRaceManager();
        Player viewer = event.getPlayer();
        RaceManager.RaceProgress progress = raceManager.getPlayerProgress().get(viewer.getUniqueId());
        if (progress == null || !progress.isFinished()) return;

        if (raceManager.teleporting.contains(target.getUniqueId())) return;

        if (raceManager.hasSpectatorHead(viewer.getUniqueId())) raceManager.removeSpectatorHead(viewer.getUniqueId());

        target.sendMessage(Component.text(GameManager.getLanguageManager().get("race.spectate_target_start", viewer.getName()), NamedTextColor.YELLOW));
        viewer.sendMessage(Component.text(GameManager.getLanguageManager().get("race.spectate_viewer_start", target.getName()), NamedTextColor.YELLOW));
    }

    @EventHandler
    public void onStopSpectate(PlayerStopSpectatingEntityEvent event) {

        if (!(event.getSpectatorTarget() instanceof Player target)) return;

        RaceManager raceManager = gameManager.getRaceManager();
        Player viewer = event.getPlayer();
        RaceManager.RaceProgress progress = raceManager.getPlayerProgress().get(viewer.getUniqueId());
        if (progress == null || !progress.isFinished()) return;

        if (raceManager.teleporting.contains(target.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(gameManager.getPlugin(), () -> viewer.setSpectatorTarget(target), 20L);
            return;
        }

        if (!raceManager.hasSpectatorHead(viewer.getUniqueId())) raceManager.createSpectatorHead(viewer);

        target.sendMessage(Component.text(GameManager.getLanguageManager().get("race.spectate_target_stop", viewer.getName()), NamedTextColor.GRAY));
        viewer.sendMessage(Component.text(GameManager.getLanguageManager().get("race.spectate_viewer_stop"), NamedTextColor.GRAY));
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
                gameManager.getPlugin().getLogger().info("Portal korrigiert von Y:" + to.getBlockY() + " zu Y:" + (highestY + 1));
            }

        } else {
            String netherName = fromWorldName.replace("castle_rush_", "castle_rush_nether_");
            World nether = Bukkit.getWorld(netherName);
            event.setCreationRadius(0);
            event.setSearchRadius(1);
            event.setTo(new Location(nether, to.getX(), to.getY(), to.getZ()));
        }
    }


}
