package com.hnp_arda.castlerush;

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import com.hnp_arda.castlerush.GameManager.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public record RaceListener(GameManager gameManager) implements Listener {

    public RaceListener(GameManager gameManager) {
        this.gameManager = gameManager;
        gameManager.getPlugin().getServer().getPluginManager().registerEvents(this, gameManager.getPlugin());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (gameManager.getGameState() != GameState.RACING) return;
        if (event.getFrom().getBlock().equals(event.getTo().getBlock())) return;


        Player player = event.getPlayer();
        RaceManager raceManager = gameManager.getRaceManager();

        raceManager.checkEndReached(player, event.getTo());

        raceManager.handleCheckpoint(player, event.getTo());

        raceManager.handleDeathzone(player, event.getTo());

        raceManager.handleEffectZone(player, event.getTo());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (gameManager.getGameState() != GameState.RACING) return;

        Player player = event.getEntity();

        event.setCancelled(true);

        RaceManager.RaceProgress progress = gameManager.getRaceManager().getPlayerProgress().get(player.getUniqueId());
        if (progress == null || progress.isFinished()) return;

        RaceManager raceManager = gameManager.getRaceManager();
        raceManager.handlePlayerDeath(player, progress, findCastleByWorld(player.getWorld()));

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


    private PlayerCastle findCastleByWorld(World world) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerCastle playerCastle = gameManager.getPlayerCastle(p);
            if (playerCastle != null && playerCastle.getCasleWorld().equals(world)) {
                return playerCastle;
            }
        }
        return null;
    }
}
