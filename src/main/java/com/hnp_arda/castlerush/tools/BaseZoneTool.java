package com.hnp_arda.castlerush.tools;

import com.hnp_arda.castlerush.core.Marker;
import com.hnp_arda.castlerush.core.PlayerCastle;
import com.hnp_arda.castlerush.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;
import java.util.function.Consumer;

import static com.hnp_arda.castlerush.core.Marker.formatLocation;

public abstract class BaseZoneTool extends BaseTool {

    private final Map<UUID, Location> zoneStarts = new HashMap<>();


    protected BaseZoneTool(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public abstract Material getToolItem();

    @Override
    public abstract void handleInteract(PlayerInteractEvent event, PlayerCastle playerCastle);

    @Override
    public void onSelect(Player player, PlayerCastle playerCastle) {
        Location startLoc = getZoneStart(player);
        if (startLoc != null) {
            sendMarker(player, startLoc, getStartMaterial(player).createBlockData());
            player.sendActionBar(Component.text(lang().get("tools.zone_tool.action_end", getDisplayName()), NamedTextColor.GOLD));
        }
        revealMarkers(player, playerCastle, getTypeId());
    }

    protected abstract Material getStartMaterial(Player player);

    protected void interact(Player player, Location location, PlayerCastle playerCastle, String data, Consumer<InteractResult> result) {

        Location start = getZoneStart(player);

        if (start == null) {
            setZoneStart(player, location.clone());
            sendMarker(player, location.clone(), getStartMaterial(player).createBlockData());
            player.sendMessage(Component.text(lang().get("tools.zone_tool.start", formatLocation(location), getDisplayName()), NamedTextColor.RED));
            player.sendActionBar(Component.text(lang().get("tools.zone_tool.action_end", getDisplayName()), NamedTextColor.GOLD));
            return;
        }
        removeZoneStart(player);

        Location end = location.clone();

        Marker startMarker = playerCastle.getMarker(start);
        Marker endMarker = playerCastle.getMarker(end);

        boolean startInZone = startMarker != null && startMarker.getData().equals(data);
        boolean endInZone = endMarker != null && endMarker.getData().equals(data);

        List<Location> regionBlocks = getBlocksBetween(start, end);


        if (startInZone && endInZone) {
            int removed = removeRegionMarkers(player, playerCastle, regionBlocks, getTypeId());
            player.sendMessage(Component.text(lang().get("tools.zone_tool.removed", removed, getDisplayName()), NamedTextColor.YELLOW));
            player.sendMessage(Component.text(lang().get("tools.zone_tool.total", playerCastle.getZoneMarker(data).size()), NamedTextColor.GRAY));
            player.sendMessage(Component.text(""));
            result.accept(InteractResult.REMOVED);
            return;
        }


        RegionChangeResult change = upsertRegionMarkers(player, playerCastle, regionBlocks, data);

        player.sendMessage(Component.text(lang().get("tools.zone_tool.end", getDisplayName(), formatLocation(location), getDisplayName()), NamedTextColor.RED));
        player.sendMessage(Component.text(lang().get("tools.zone_tool.added", change.added(), getDisplayName()), NamedTextColor.RED));
        player.sendMessage(Component.text(lang().get("tools.zone_tool.total", playerCastle.getZoneMarker(data).size()), NamedTextColor.GRAY));

        if (change.replaced() > 0) {
            player.sendMessage(Component.text(lang().get("tools.zone_tool.replaced_total", change.replaced()), NamedTextColor.YELLOW));
            player.sendMessage(Component.text(lang().get("tools.zone_tool.replaced_list", String.join(", ", change.replacedTypes())), NamedTextColor.GRAY));
            result.accept(InteractResult.REPLACED);
        } else result.accept(InteractResult.PLACED);

        player.sendMessage(Component.text(""));


    }


    protected List<Location> getBlocksBetween(Location loc1, Location loc2) {
        List<Location> blocks = new ArrayList<>();

        int minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(loc1.getWorld(), x, y, z);
                    blocks.add(loc);
                }
            }
        }

        return blocks;
    }

    protected int removeRegionMarkers(Player player, PlayerCastle playerCastle, List<Location> regionBlocks, String typeId) {
        int removed = 0;
        for (Location loc : regionBlocks) {
            Marker marker = playerCastle.getMarker(loc);
            if (marker != null && marker.getTypeId().equalsIgnoreCase(typeId)) {
                sendMarker(player, marker.getLocation(), marker.getOriginalMaterial().createBlockData());
                playerCastle.removeMarker(marker);
                removed++;
            }
        }
        return removed;
    }

    protected RegionChangeResult upsertRegionMarkers(Player player, PlayerCastle playerCastle, List<Location> regionBlocks, String data) {
        int added = 0;
        int replaced = 0;
        List<String> replacedTypes = new ArrayList<>();

        for (Location loc : regionBlocks) {
            Marker existingMarker = playerCastle.getMarker(loc);
            if (existingMarker != null && !existingMarker.getData().equals(data)) {
                playerCastle.removeMarker(existingMarker);
                replaced++;
                String replacedName = lang().get(existingMarker.getTranslationKey());
                if (!replacedTypes.contains(replacedName)) {
                    replacedTypes.add(replacedName);
                }
            }

            if (existingMarker != null && existingMarker.getData().equals(data)) {
                sendMarker(player, existingMarker.getLocation(), existingMarker.getDisplayMaterial().createBlockData());
            } else {
                Marker marker = new Marker(this, loc.clone(), data);
                playerCastle.addMarker(marker);
                sendMarker(player, marker.getLocation(), marker.getDisplayMaterial().createBlockData());
            }
            added++;
        }

        return new RegionChangeResult(added, replaced, replacedTypes);
    }

    @Override
    public boolean isReplaceable() {
        return false;
    }

    protected Location getZoneStart(Player player) {
        if (!zoneStarts.containsKey(player.getUniqueId())) return null;
        return zoneStarts.get(player.getUniqueId());
    }

    protected void setZoneStart(Player player, Location location) {
        zoneStarts.put(player.getUniqueId(), location);
    }

    protected void removeZoneStart(Player player) {
        zoneStarts.remove(player.getUniqueId());
    }

    protected record RegionChangeResult(int added, int replaced, List<String> replacedTypes) {
    }

}
