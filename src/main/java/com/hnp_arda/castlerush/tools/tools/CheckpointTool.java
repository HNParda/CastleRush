package com.hnp_arda.castlerush.tools.tools;

import com.hnp_arda.castlerush.managers.GameManager;
import com.hnp_arda.castlerush.PlayerCastle;
import com.hnp_arda.castlerush.tools.BaseTool;
import com.hnp_arda.castlerush.tools.MarkerData;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class CheckpointTool extends BaseTool {


    public CheckpointTool(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public Material getToolItem() {
        return Material.GOLDEN_SHOVEL;
    }

    @Override
    public String getName() {
        return "Checkpoint";
    }

    @Override
    public void handleInteract(PlayerInteractEvent event, PlayerCastle playerCastle) {
        if (event.getClickedBlock() == null) return;
        placeSimpleMarker(event.getPlayer(), playerCastle, getTypeId(), event.getClickedBlock().getLocation());
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
        return Material.GOLD_BLOCK;
    }

    @Override
    public void triggerEnter(Player player, MarkerData marker) {
        if (player.getGameMode() == GameMode.SURVIVAL)
            gameManager.getRaceManager().setCheckpoint(player, marker.getLocation());
    }

    @Override
    public void triggerExit(Player player) {

    }
}
