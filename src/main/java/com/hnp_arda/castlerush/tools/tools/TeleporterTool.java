package com.hnp_arda.castlerush.tools.tools;

import com.destroystokyo.paper.ParticleBuilder;
import com.hnp_arda.castlerush.core.Marker;
import com.hnp_arda.castlerush.core.PlayerCastle;
import com.hnp_arda.castlerush.managers.GameManager;
import com.hnp_arda.castlerush.tools.BaseTool;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.hnp_arda.castlerush.core.Marker.formatLocation;

public class TeleporterTool extends BaseTool {

    private final HashMap<UUID, BukkitTask> particles = new HashMap<>();
    HashMap<UUID, Location> selectedTargets = new HashMap<>();
    HashMap<UUID, Integer> selectedIndex = new HashMap<>();

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

        Marker oldMarker = playerCastle.getMarker(location);
        if (oldMarker != null && !oldMarker.getTypeId().equals(getTypeId())) oldMarker = null;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            Location target = selectedTargets.getOrDefault(player.getUniqueId(), null);
            int index = selectedIndex.getOrDefault(player.getUniqueId(), 0);
            if (index != 0 && oldMarker == null) {
                player.sendMessage(Component.text(lang().get("tools.teleporter.messages.select_add_new"), NamedTextColor.RED));
                event.setCancelled(true);
                return;
            } else

            if (target == null && oldMarker == null) {
                player.sendMessage(Component.text(lang().get("tools.teleporter.messages.not_selected"), NamedTextColor.RED));
                event.setCancelled(true);
                return;
            }
            interact(player, playerCastle, getTypeId(), location, (result) -> {
                if (result.equals(InteractResult.REMOVED)) {
                    Marker marker = playerCastle.getMarker(location);
                    hideTeleporter(player, marker);

                    List<Marker> markers = getTeleporters(player);
                    Marker selectedMarker = markers.get(selectedIndex.get(player.getUniqueId()));
                    if (marker .equals(selectedMarker) ) selectedIndex.put(player.getUniqueId(), 0);

                } else if (!result.equals(InteractResult.CANCELED)) {
                    Marker marker = playerCastle.getMarker(location);
                    if (target == null) {
                        event.setCancelled(true);
                        return;
                    }
                    marker.setData(getTypeId() + ";" + toString(target));

                    List<Marker> markers = getTeleporters(player);

                    selectedTargets.put(player.getUniqueId(), null);
                    selectedIndex.put(player.getUniqueId(), markers.size() - 1);
                    showTeleporter(player, marker);
                }
            });

        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (oldMarker != null) {
                player.sendMessage(Component.text(lang().get("tools.teleporter.messages.invalid_target", NamedTextColor.RED)));
                event.setCancelled(true);
                return;
            }
            int index = selectedIndex.getOrDefault(player.getUniqueId(), 0);
            if (index != 0) {
                List<Marker> markers = getTeleporters(player);
                Marker marker = markers.get(index);
                hideTeleporter(player, marker);
                player.sendMessage(Component.text(lang().get("tools.teleporter.messages.updated_target", formatLocation(getLocation(marker.getData())), formatLocation(location)), NamedTextColor.GREEN));
                marker.setData(getTypeId() + ";" + toString(location));
                showTeleporter(player, marker);
                return;
            }
            selectedTargets.put(player.getUniqueId(), location);
            player.sendMessage(Component.text(lang().get("tools.teleporter.messages.selected_target", formatLocation(location)), NamedTextColor.GREEN));
        }

    }

    private Location getLocation(String data) {
        String[] loc = data.split(Pattern.quote("%splitter%&$"));
        return new Location(Bukkit.getWorld(loc[0]), Double.parseDouble(loc[1]), Double.parseDouble(loc[2]), Double.parseDouble(loc[3]));
    }

    private String toString(Location loc) {
        return loc.getWorld().getName() + "%splitter%&$" + loc.getX() + "%splitter%&$" + loc.getY() + "%splitter%&$" + loc.getZ();
    }

    private void hideTeleporter(Player player, Marker marker) {
        String data = marker.getData();
        Location loc = getLocation(data.replace(getTypeId() + ";", ""));
        player.sendBlockChange(loc, player.getWorld().getBlockAt(loc).getType().createBlockData());

        BukkitTask task = particles.get(player.getUniqueId());
        if (task != null) task.cancel();
    }

    private void showTeleporter(Player player, Marker marker) {
        Location loc = getLocation(marker.getData().split(";")[1]);
        player.sendBlockChange(loc, Material.PRISMARINE_BRICKS.createBlockData());

        ParticleBuilder particleBuilder = Particle.DUST.builder().color(Color.PURPLE, 2.0f).count(0).extra(0.1);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(gameManager.getPlugin(), () -> {

            for (double i = 1; i <= 3.0; i += 0.25) {
                particleBuilder.location(marker.getLocation().clone().add(.5, i, .5)).receivers(32, true).spawn();
                particleBuilder.location(loc.clone().add(.5, i, .5)).receivers(32, true).spawn();
            }
        }, 0, 4);

        particles.put(player.getUniqueId(), task);
    }

    private List<Marker> getTeleporters(Player player) {

        List<Marker> markers = new ArrayList<>();
        markers.add(null);
        markers.addAll(gameManager.getPlayerCastle(player).getMarkers(getTypeId()));
        return markers;
    }

    @Override
    public boolean scrollEvent(Player player, int i) {

        List<Marker> markers = getTeleporters(player);

        int index = selectedIndex.getOrDefault(player.getUniqueId(), 0);

        if (index != 0) hideTeleporter(player, markers.get(index));

        int newIndex = index + i;
        if (newIndex >= markers.size()) newIndex = 0;
        else if (newIndex == -1) newIndex = markers.size() - 1;

        selectedIndex.put(player.getUniqueId(), newIndex);

        if (newIndex != 0) {
            player.sendActionBar(Component.text(lang().get("tools.teleporter.messages.selected"), NamedTextColor.YELLOW).append(Component.text("#" + newIndex, NamedTextColor.DARK_PURPLE)));
            showTeleporter(player, markers.get(newIndex));
        } else
            player.sendActionBar(Component.text(lang().get("tools.teleporter.messages.add_new"), NamedTextColor.YELLOW));

        return true;
    }

    @Override
    public void onSelect(Player player, PlayerCastle playerCastle) {
        selectedIndex.put(player.getUniqueId(), 0);
        player.sendActionBar(Component.text(lang().get("tools.teleporter.messages.add_new"), NamedTextColor.YELLOW));
        super.onSelect(player, playerCastle);
    }

    @Override
    public void onDeselect(Player player, PlayerCastle playerCastle) {

        List<Marker> markers = getTeleporters(player);

        int index = selectedIndex.getOrDefault(player.getUniqueId(), 0);

        if (index != 0) hideTeleporter(player, markers.get(index));

        player.sendActionBar(Component.empty());

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

        String data = marker.getData();
        Location loc = getLocation(data.replace(getTypeId() + ";", ""));

        player.teleport(loc.clone().add(.5, 1, .5));

        Marker m = gameManager.getPlayerCastle(player).getMarker(loc);
        if (m != null) m.triggerMarkerEnter(player);

    }

    @Override
    public void triggerExit(Player player) {

    }
}
