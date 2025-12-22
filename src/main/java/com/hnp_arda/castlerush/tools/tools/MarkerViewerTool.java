package com.hnp_arda.castlerush.tools.tools;

import com.hnp_arda.castlerush.managers.GameManager;
import com.hnp_arda.castlerush.PlayerCastle;
import com.hnp_arda.castlerush.tools.BaseTool;
import com.hnp_arda.castlerush.tools.MarkerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class MarkerViewerTool extends BaseTool {

    public MarkerViewerTool(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public String getName() {
        return "Markers";
    }

    @Override
    public void triggerEnter(Player player, MarkerData marker) {

    }

    @Override
    public void triggerExit(Player player) {

    }

    @Override
    public Material getToolItem() {
        return Material.GOLD_INGOT;
    }

    @Override
    public void handleInteract(PlayerInteractEvent event, PlayerCastle playerCastle) {}

    @Override
    public void onSelect(Player player, PlayerCastle playerCastle) {
        revealAllMarkers(player, playerCastle);
    }

    @Override
    public void onDeselect(Player player, PlayerCastle playerCastle) {
        hideAllMarkers(player, playerCastle);
    }
}
