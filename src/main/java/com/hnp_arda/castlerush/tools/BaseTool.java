package com.hnp_arda.castlerush.tools;

import com.hnp_arda.castlerush.core.Marker;
import com.hnp_arda.castlerush.core.PlayerCastle;
import com.hnp_arda.castlerush.managers.GameManager;
import com.hnp_arda.castlerush.managers.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.function.Consumer;

import static com.hnp_arda.castlerush.managers.GameManager.languageManager;
import static com.hnp_arda.castlerush.core.Marker.formatLocation;

public abstract class BaseTool {

    protected final GameManager gameManager;

    protected BaseTool(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public abstract Material getToolItem();

    public abstract void handleInteract(PlayerInteractEvent event, PlayerCastle playerCastle);

    public void onSelect(Player player, PlayerCastle playerCastle) {
    }

    public void onDeselect(Player player, PlayerCastle playerCastle) {
    }

    protected LanguageManager lang() {
        return GameManager.getLanguageManager();
    }

    protected void sendMarker(Player player, Location location, BlockData block) {
        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendBlockChange(location, block);
            }
        }.runTaskLater(gameManager.getPlugin(), 2L);
    }

    protected void revealMarkers(Player player, PlayerCastle playerCastle, String markerType) {
        playerCastle.getMarkers(markerType).forEach(marker ->
                sendMarker(player, marker.getLocation(), marker.getDisplayMaterial().createBlockData()));
    }

    protected void hideMarkers(Player player, PlayerCastle playerCastle, String markerType) {
        playerCastle.getMarkers(markerType).forEach(marker ->
                sendMarker(player, marker.getLocation(), marker.getOriginalMaterial().createBlockData()));
    }

    protected void revealAllMarkers(Player player, PlayerCastle playerCastle) {
        playerCastle.getMarkers().stream().map(Marker::getTypeId).distinct().forEach(type -> revealMarkers(player, playerCastle, type));
    }

    public boolean isReplacable() {
        return true;
    }

    protected void hideAllMarkers(Player player, PlayerCastle playerCastle) {
        playerCastle.getMarkers().stream().map(Marker::getTypeId).distinct().forEach(type -> hideMarkers(player, playerCastle, type));
    }

    protected void interact(Player player, PlayerCastle playerCastle, String typeId, Location location, Consumer<InteractResult> result) {

        player.sendMessage("");
        Marker existingAtLocation = playerCastle.getLocation(location);

        if (existingAtLocation == null) {
            removeOldIfSingleMarker(player, playerCastle);
            Marker marker = new Marker(this, location.clone());
            playerCastle.addMarker(marker);

            player.sendActionBar(Component.text(lang().get("tools.placed", getDisplayName()), NamedTextColor.GOLD));
            player.sendMessage(Component.text(lang().get("tools.placed_detail", getDisplayName(), formatLocation(marker.getLocation())), NamedTextColor.GREEN));

            revealMarkers(player, playerCastle, getTypeId());
            player.sendMessage("");
            if (result != null) result.accept(InteractResult.PLACED);
            return;
        }


        if (!existingAtLocation.getTypeId().equals(typeId)) {

            if (!existingAtLocation.isReplaceable()) {
                String existingName = lang().get(existingAtLocation.getTranslationKey());
                player.sendMessage(Component.text(lang().get("tools.not_replaceable", getDisplayName(), existingName), NamedTextColor.RED));
                player.sendMessage("");
                if (result != null) result.accept(InteractResult.CANCELED);
                return;
            }

            removeOldIfSingleMarker(player, playerCastle);

            playerCastle.removeMarker(existingAtLocation);

            Marker marker = new Marker(this, location.clone());
            playerCastle.addMarker(marker);

            player.sendActionBar(Component.text(lang().get("tools.replaced", getDisplayName()), NamedTextColor.GOLD));
            player.sendMessage(Component.text(lang().get("tools.placed_detail", getDisplayName(), formatLocation(marker.getLocation())), NamedTextColor.YELLOW));

            if (result != null) result.accept(InteractResult.REPLACED);
        } else {
            player.sendActionBar(Component.text(lang().get("tools.removed", getDisplayName()), NamedTextColor.GOLD));
            player.sendMessage(Component.text(lang().get("tools.removed_detail", getDisplayName(), formatLocation(existingAtLocation.getLocation())), NamedTextColor.GREEN));
            playerCastle.removeMarker(existingAtLocation);

            if (result != null) result.accept(InteractResult.REMOVED);
        }

        revealMarkers(player, playerCastle, getTypeId());
        player.sendMessage("");

    }

    private void removeOldIfSingleMarker(Player player, PlayerCastle playerCastle) {
        Marker oldMarker = getSingleOnly(playerCastle);
        if (oldMarker != null) {
            String displayName = lang().get(oldMarker.getTranslationKey());
            player.sendMessage(Component.text(lang().get("tools.single_marker", displayName, formatLocation(oldMarker.getLocation())), NamedTextColor.RED));
            sendMarker(player, oldMarker.getLocation(), oldMarker.getOriginalMaterial().createBlockData());
            playerCastle.removeMarker(oldMarker);
        }
    }

    private Marker getSingleOnly(PlayerCastle playerCastle) {
        if (!singleOnly()) return null;
        List<Marker> markers = playerCastle.getMarkers(this.getTypeId());
        if (markers.isEmpty()) return null;
        return markers.getFirst();
    }

    protected boolean singleOnly() {
        return false;
    }

    public String getName() {
        return "Tool";
    }

    public String getTypeId() {
        return getName().toLowerCase();
    }

    public String getTranslationKey() {
        return "tools." + getTypeId() + ".name";
    }

    public String getDisplayName() {
        return languageManager.get(getTranslationKey());
    }

    public String getReceived() {
        return String.format(languageManager.get("tools.received"), getDisplayName());
    }

    public String getDisplay() {
        return String.format(languageManager.get("tools.display"), languageManager.get("tools." + getName().toLowerCase() + ".display"));
    }

    public String getInfo() {
        return languageManager.get("tools." + getName().toLowerCase() + ".info");
    }

    public String getCommand() {
        return languageManager.get("tools." + getName().toLowerCase() + ".cmd");
    }

    public void giveToPlayer(Player player) {
        ItemStack item = new ItemStack(getToolItem());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(getDisplayName(), NamedTextColor.YELLOW));
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        player.getInventory().addItem(item);
    }

    public Material getDisplayMaterial(World world, Marker marker) {
        return world.getBlockAt(marker.getLocation()).getType();
    }

    public abstract void triggerEnter(Player player, Marker marker);

    public abstract void triggerExit(Player player);

    public boolean canLeftClick() {
        return false;
    }

    protected enum InteractResult {
        PLACED, REMOVED, REPLACED, CANCELED
    }

}
