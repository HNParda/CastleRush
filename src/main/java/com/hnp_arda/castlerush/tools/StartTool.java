package com.hnp_arda.castlerush.tools;

import com.hnp_arda.castlerush.PlayerCastle;
import com.hnp_arda.castlerush.GameManager;
import com.hnp_arda.castlerush.RaceManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class StartTool extends Tool {

    public StartTool(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public String getName() {
        return "Start";
    }

    @Override
    public Material getToolItem() {
        return Material.GOLDEN_PICKAXE;
    }

    @Override
    public void handleInteract(PlayerInteractEvent event, PlayerCastle playerCastle) {
        if (event.getClickedBlock() == null) return;
        event.getPlayer().sendMessage("");
        playerCastle.setStart(event.getClickedBlock().getLocation());
        placeSimpleMarker(event.getPlayer(), playerCastle, getTypeId(), event.getClickedBlock().getLocation());
        revealMarkers(event.getPlayer(), playerCastle, getTypeId());
        event.getPlayer().sendMessage("");
    }

    @Override
    public void onSelect(Player player, PlayerCastle playerCastle) {
        revealMarkers(player, playerCastle, getTypeId());
    }

    @Override
    public void onDeselect(Player player, PlayerCastle playerCastle) {
        hideMarkers(player, playerCastle, getTypeId());
    }

    @Override
    protected Material getDisplayMaterial(World world, MarkerData marker) {
        return Material.EMERALD_BLOCK;
    }

    @Override
    public void triggerEnter(Player player, MarkerData marker) {

        if (player.getGameMode() == GameMode.SPECTATOR && player.getSpectatorTarget() == null) {
            RaceManager.RaceProgress progress = gameManager.getRaceManager().getPlayerProgress().get(player.getUniqueId());
            int castleIndex = progress.getCurrentCastle();

            if (castleIndex == 0) return;

            PlayerCastle playerCastle = gameManager.getRaceManager().getCastle(castleIndex - 1);
            Location target = playerCastle.getEnd();
            if (target == null) return;
            player.teleport(target.clone().add(0.5, 1, 0.5));
            return;
        }


        gameManager.getRaceManager().setCheckpoint(player, marker.getLocation());
    }


    @Override
    public void triggerExit(Player player) {

    }
}
