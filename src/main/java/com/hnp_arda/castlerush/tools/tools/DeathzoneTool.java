package com.hnp_arda.castlerush.tools.tools;

import com.hnp_arda.castlerush.managers.GameManager;
import com.hnp_arda.castlerush.PlayerCastle;
import com.hnp_arda.castlerush.tools.BaseAdvancedTool;
import com.hnp_arda.castlerush.tools.MarkerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

import static com.hnp_arda.castlerush.tools.MarkerData.formatLocation;

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

        if (!playerCastle.hasDeathzoneStart()) {
            playerCastle.setDeathzoneStart(location.clone());
            sendMarker(player, location.clone(), Material.ORANGE_CONCRETE.createBlockData());
            String name = lang().get("tools.deathzone.name");
            player.sendMessage(Component.text(lang().get("tools.advanced_tool.start", formatLocation(location), name), NamedTextColor.RED));
            player.sendActionBar(Component.text(lang().get("tools.advanced_tool.action_end", name), NamedTextColor.GOLD));
            return;
        }

        Location start = playerCastle.getDeathzoneStart();
        Location end = location.clone();

        boolean startInZone = playerCastle.isDeathzoneBlock(start);
        boolean endInZone = playerCastle.isDeathzoneBlock(end);

        List<Location> regionBlocks = getBlocksBetween(start, end);

        RegionToggleResult result = toggleRegionMarkers(
                player,
                playerCastle,
                regionBlocks,
                getTypeId(),
                startInZone && endInZone,
                loc -> new MarkerData(this, loc.clone(), getTypeId(), "tools.deathzone.name", "deathzone")
        );

        player.sendMessage(Component.text(""));

        if (result.removedMode()) {
            String name = lang().get("tools.deathzone.name");
            player.sendMessage(Component.text(lang().get("tools.advanced_tool.removed", result.removedCount(), name), NamedTextColor.YELLOW));
            player.sendMessage(Component.text(lang().get("tools.advanced_tool.total", playerCastle.getMarker(getTypeId()).size()), NamedTextColor.GRAY));
            player.sendMessage(Component.text(""));
            player.sendMessage(Component.text(""));
        } else {
            RegionChangeResult change = result.change();
            String name = lang().get("tools.deathzone.name");
            player.sendMessage(Component.text(lang().get("tools.advanced_tool.end", name, formatLocation(location), name), NamedTextColor.RED));
            player.sendMessage(Component.text(lang().get("tools.advanced_tool.added", change.added(), name), NamedTextColor.RED));
            player.sendMessage(Component.text(lang().get("tools.advanced_tool.total", playerCastle.getMarker(getTypeId()).size()), NamedTextColor.GRAY));

            if (change.replaced() > 0) {
                player.sendMessage(Component.text(""));
                player.sendMessage(Component.text(lang().get("tools.advanced_tool.replaced_total", change.replaced()), NamedTextColor.YELLOW));
                player.sendMessage(Component.text(lang().get("tools.advanced_tool.replaced_list", String.join(", ", change.replacedTypes())), NamedTextColor.GRAY));
            }

            player.sendMessage(Component.text(""));
        }

        playerCastle.setDeathzoneStart(null);
    }

    @Override
    public void onSelect(Player player, PlayerCastle playerCastle) {
        if (playerCastle.hasDeathzoneStart()) {
            Location startLoc = playerCastle.getDeathzoneStart();
            sendMarker(player, startLoc, Material.ORANGE_CONCRETE.createBlockData());
            player.sendActionBar(Component.text(lang().get("tools.advanced_tool.action_end", lang().get("tools.deathzone.name")), NamedTextColor.GOLD));
        }
        revealMarkers(player, playerCastle, getTypeId());
    }

    @Override
    public void onDeselect(Player player, PlayerCastle playerCastle) {
        hideMarkers(player, playerCastle, getTypeId());
        if (playerCastle.hasDeathzoneStart()) {
            Location start = playerCastle.getDeathzoneStart();
            player.sendBlockChange(start, start.getBlock().getBlockData());
        }
    }

    @Override
    protected Material getDisplayMaterial(World world, MarkerData marker) {
        return marker.isAir() ? Material.RED_STAINED_GLASS : Material.REDSTONE_BLOCK;
    }

    @Override
    public void triggerEnter(Player player, MarkerData marker) {
        if (player.getGameMode() == GameMode.SURVIVAL)
            gameManager.getRaceManager().handlePlayerDeath(player);
    }

    @Override
    public void triggerExit(Player player) {

    }
}
