package com.hnp_arda.castlerush.tools;

import com.hnp_arda.castlerush.GameManager;
import com.hnp_arda.castlerush.PlayerCastle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class AdvancedTool extends Tool{

    protected AdvancedTool(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public abstract Material getToolItem();

    @Override
    public abstract void handleInteract(PlayerInteractEvent event, PlayerCastle playerCastle);


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
            MarkerData marker = playerCastle.getLocation(loc);
            if (marker != null && marker.getTypeId().equalsIgnoreCase(typeId)) {
                sendMarker(player, marker.getLocation(), marker.getOriginalMaterial().createBlockData());
                playerCastle.removeMarker(marker);
                removed++;
            }
        }
        return removed;
    }

    protected RegionChangeResult upsertRegionMarkers(Player player,
                                                     PlayerCastle playerCastle,
                                                     List<Location> regionBlocks,
                                                     String typeId,
                                                     Function<Location, MarkerData> markerFactory
                                                   //  ,BiConsumer<MarkerData, Material> existingUpdater
    ) {
        int added = 0;
        int replaced = 0;
        List<String> replacedTypes = new ArrayList<>();

        for (Location loc : regionBlocks) {
            MarkerData existingMarker = playerCastle.getLocation(loc);
            if (existingMarker != null && !existingMarker.getTypeId().equalsIgnoreCase(typeId)) {
                playerCastle.removeMarker(existingMarker);
                replaced++;
                String replacedName = lang().get(existingMarker.getTranslationKey());
                if (!replacedTypes.contains(replacedName)) {
                    replacedTypes.add(replacedName);
                }
            }

            Material original = loc.getBlock().getType();
            if (existingMarker != null && existingMarker.getTypeId().equalsIgnoreCase(typeId)) {
               /* existingMarker.setOriginalMaterial(original);*/
              //  existingUpdater.accept(existingMarker);
                sendMarker(player, existingMarker.getLocation(), existingMarker.getDisplayMaterial().createBlockData());
            } else {
                MarkerData marker = markerFactory.apply(loc);
                playerCastle.addMarker(marker);
                sendMarker(player, marker.getLocation(), marker.getDisplayMaterial().createBlockData());
            }
            added++;
        }

        return new RegionChangeResult(added, replaced, replacedTypes);
    }

    protected RegionToggleResult toggleRegionMarkers(Player player,
                                                     PlayerCastle playerCastle,
                                                     List<Location> regionBlocks,
                                                     String typeId,
                                                     boolean removeMode,
                                                     Function<Location, MarkerData> markerFactory
                                                 //   , BiConsumer<MarkerData, Material> existingUpdater
    ) {
        if (removeMode) {
            int removed = removeRegionMarkers(player, playerCastle, regionBlocks, typeId);
            return new RegionToggleResult(true, removed, new RegionChangeResult(0, 0, List.of()));
        }
        RegionChangeResult change = upsertRegionMarkers(player, playerCastle, regionBlocks, typeId, markerFactory//, existingUpdater
                 );
        return new RegionToggleResult(false, 0, change);
    }

    protected record RegionChangeResult(int added, int replaced, List<String> replacedTypes) {}

    protected record RegionToggleResult(boolean removedMode, int removedCount, RegionChangeResult change) {}

}
