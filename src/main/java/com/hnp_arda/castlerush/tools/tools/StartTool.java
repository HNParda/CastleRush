package com.hnp_arda.castlerush.tools.tools;

import com.hnp_arda.castlerush.core.PlayerCastle;
import com.hnp_arda.castlerush.managers.GameManager;
import com.hnp_arda.castlerush.managers.RaceManager;
import com.hnp_arda.castlerush.tools.BaseTool;
import com.hnp_arda.castlerush.core.Marker;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class StartTool extends BaseTool {

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

        interact(event.getPlayer(), playerCastle, getTypeId(), event.getClickedBlock().getLocation(), (result) -> {
            if (result.equals(InteractResult.REMOVED)) playerCastle.setStart(playerCastle.getCasleWorld().getSpawnLocation());
            else if (!result.equals(InteractResult.CANCELED))
                playerCastle.setStart(event.getClickedBlock().getLocation());
        });
    }

    @Override
    public boolean isReplaceable() {
        return false;
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
    public Material getDisplayMaterial(World world, Marker marker) {
        return Material.EMERALD_BLOCK;
    }

    @Override
    public void triggerEnter(Player player, Marker marker) {

        if (player.getGameMode() == GameMode.SPECTATOR && player.getSpectatorTarget() == null) {
            RaceManager.RaceProgress progress = gameManager.getRaceManager().getPlayerProgress().get(player.getUniqueId());
            int castleIndex = progress.getCurrentCastle();

            if (castleIndex == 0) return;

            PlayerCastle playerCastle = gameManager.getRaceManager().getCastle(castleIndex - 1);
            Location target = playerCastle.getEnd();
            if (target == null) return;
            player.teleport(target);
            return;
        }


        gameManager.getRaceManager().setCheckpoint(player, marker.getLocation());
    }

    @Override
    protected boolean singleOnly() {
        return true;
    }

    @Override
    public void triggerExit(Player player) {

    }
}
