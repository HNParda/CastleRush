package com.hnp_arda.castlerush.tools.tools;

import com.hnp_arda.castlerush.core.Marker;
import com.hnp_arda.castlerush.core.PlayerCastle;
import com.hnp_arda.castlerush.managers.GameManager;
import com.hnp_arda.castlerush.tools.BaseTool;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;
import java.util.regex.Pattern;

public class TeleporterTool extends BaseTool {

    HashMap<UUID, Location> selectedTargets = new HashMap<>();
    HashMap<UUID, Integer> selectedIindex = new HashMap<>();

    public TeleporterTool(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public String getName() {
        return "Teleporter";
    }

    @Override
    public Material getToolItem() {
        return Material.GOLDEN_HORSE_ARMOR;
    }

    @Override
    public void handleInteract(PlayerInteractEvent event, PlayerCastle playerCastle) {
        if (event.getClickedBlock() == null) return;

        Location location = event.getClickedBlock().getLocation();
        Player player = event.getPlayer();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            interact(player, playerCastle, getTypeId(), location, (result) -> {
                if (result.equals(InteractResult.REMOVED)) {
                    Marker marker = playerCastle.getMarker(location);
                    hideTeleporter(player, marker);

                } else if (!result.equals(InteractResult.CANCELED)) {
                    Marker marker = playerCastle.getMarker(location);
                    Location target = selectedTargets.getOrDefault(player.getUniqueId(), null);
                    if (target == null) {

                        return;
                    }
                    marker.setAdditionalData(getTypeId() + ";" + toString(target));
                    showTeleporter(player, marker);
                }
            });

        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (event.getClickedBlock() == null) return;
            selectedTargets.put(player.getUniqueId(), location);
            player.sendMessage("seelcted .");
        }

    }

    private Location getLocation(String string) {
        String[] loc = string.split(Pattern.quote("%splitter%&$"));
        gameManager.getPlugin().getLogger().info(string + "    " + Arrays.toString(loc));
        return new Location(Bukkit.getWorld(loc[0]), Double.parseDouble(loc[1]), Double.parseDouble(loc[2]), Double.parseDouble(loc[3]));
    }

    private String toString(Location loc) {
        return loc.getWorld().getName() + "%splitter%&$" + loc.getX() + "%splitter%&$" + loc.getY() + "%splitter%&$" + loc.getZ();
    }

    private void hideTeleporter(Player player, Marker marker) {
        String additionalData = marker.getAdditionalToolData();
        Location loc = getLocation(additionalData.replace(getTypeId() + ";", ""));
        player.sendBlockChange(loc, player.getWorld().getBlockAt(loc).getType().createBlockData());
    }

    private void showTeleporter(Player player, Marker marker) {
        Location loc = getLocation(marker.getAdditionalToolData().split(";")[1]);
        gameManager.getPlugin().getLogger().info(loc.toString());
        player.sendBlockChange(loc, Material.PRISMARINE_BRICKS.createBlockData());
        //player.getWorld().spawnParticle();
    }

    @Override
    public boolean scrollEvent(Player player, int i) {

        List<Marker> markers = new ArrayList<>();
        markers.add(null);
        markers.addAll(gameManager.getPlayerCastle(player).getMarkers(getTypeId()));

        int index = selectedIindex.getOrDefault(player.getUniqueId(), 0);

        if (index != 0)
            hideTeleporter(player, markers.get(index));

        int newIndex = index + i;
        if (newIndex == markers.size()) newIndex = 0;
        else if (newIndex == -1) newIndex = markers.size() - 1;

        selectedIindex.put(player.getUniqueId(), newIndex);

        if (newIndex != 0)
            showTeleporter(player, markers.get(newIndex));

        return true;
    }

    @Override
    public void onSelect(Player player, PlayerCastle playerCastle) {

        super.onSelect(player, playerCastle);
    }

    @Override
    public void onDeselect(Player player, PlayerCastle playerCastle) {

        super.onDeselect(player, playerCastle);
    }

    @Override
    public boolean canLeftClick() {
        return true;
    }

    @Override
    public Material getDisplayMaterial(World world, Marker marker) {
        return Material.BEACON;
    }

    @Override
    public void triggerEnter(Player player, Marker marker) {


        String additionalData = marker.getAdditionalToolData();
        Location loc = getLocation(additionalData.replace(getTypeId() + ";", ""));

        player.teleport(loc.add(.5, 1, .5));

    }

    @Override
    public void triggerExit(Player player) {

    }
}
