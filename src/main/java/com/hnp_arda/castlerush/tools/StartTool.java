package com.hnp_arda.castlerush.tools;

import com.hnp_arda.castlerush.PlayerCastle;
import com.hnp_arda.castlerush.GameManager;
import org.bukkit.Material;
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
    protected Material getDisplayMaterial(Material original) {
        return Material.EMERALD_BLOCK;
    }
}
