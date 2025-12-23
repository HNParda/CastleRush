package com.hnp_arda.castlerush.tools.tools;

import com.hnp_arda.castlerush.core.PlayerCastle;
import com.hnp_arda.castlerush.managers.GameManager;
import com.hnp_arda.castlerush.tools.BaseAdvancedTool;
import com.hnp_arda.castlerush.core.Marker;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class DeathzoneTool extends BaseAdvancedTool {

    public DeathzoneTool(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public String getName() {
        return "Deathzone";
    }

    @Override
    public Material getToolItem() {
        return Material.GOLDEN_HOE;
    }

    @Override
    public void handleInteract(PlayerInteractEvent event, PlayerCastle playerCastle) {
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        Location location = event.getClickedBlock().getLocation();

        String advancedToolData = getTypeId();

        interact(player, location, playerCastle, advancedToolData, result -> {
        });

        player.sendMessage(Component.text(""));

    }

    @Override
    public void onDeselect(Player player, PlayerCastle playerCastle) {
        hideMarkers(player, playerCastle, getTypeId());
        Location startLoc = getZoneStart(player);
        if (startLoc != null) player.sendBlockChange(startLoc, startLoc.getBlock().getBlockData());

    }

    @Override
    public Material getDisplayMaterial(World world, Marker marker) {
        return marker.isAir() ? Material.RED_STAINED_GLASS : Material.REDSTONE_BLOCK;
    }

    @Override
    protected Material getStartMaterial(Player _ignored) {
        return Material.ORANGE_CONCRETE;
    }

    @Override
    public void triggerEnter(Player player, Marker marker) {
        if (player.getGameMode() == GameMode.SURVIVAL) gameManager.getRaceManager().handlePlayerDeath(player);
    }

    @Override
    public void triggerExit(Player player) {

    }
}
