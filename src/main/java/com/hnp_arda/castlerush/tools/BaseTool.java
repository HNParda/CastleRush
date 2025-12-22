package com.hnp_arda.castlerush.tools;

import com.hnp_arda.castlerush.managers.GameManager;
import com.hnp_arda.castlerush.managers.LanguageManager;
import com.hnp_arda.castlerush.PlayerCastle;
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

import static com.hnp_arda.castlerush.managers.GameManager.languageManager;

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
        new BukkitRunnable() {
            @Override
            public void run() {
                playerCastle.getMarker(markerType).forEach(marker -> {
                    gameManager.getPlugin().getLogger().info("aaaa " +  marker.getDisplayMaterial() + "  " + getDisplayMaterial(player.getWorld(), marker));
                    sendMarker(player, marker.getLocation(), marker.getDisplayMaterial().createBlockData());
                });
            }
        }.runTaskLater(gameManager.getPlugin(), 2L);
    }

    protected void hideMarkers(Player player, PlayerCastle playerCastle, String markerType) {
        playerCastle.getMarker(markerType).forEach(marker -> player.sendBlockChange(marker.getLocation(), marker.getOriginalMaterial().createBlockData()));
    }

    protected void revealAllMarkers(Player player, PlayerCastle playerCastle) {
        playerCastle.getMarkers().stream().map(MarkerData::getTypeId).distinct().forEach(type -> revealMarkers(player, playerCastle, type));
    }

    protected void hideAllMarkers(Player player, PlayerCastle playerCastle) {
        playerCastle.getMarkers().stream().map(MarkerData::getTypeId).distinct().forEach(type -> hideMarkers(player, playerCastle, type));
    }
// ANDERN!!!!!!!!! aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    protected void placeSimpleMarker(Player player, PlayerCastle playerCastle, String typeId, Location location) {

        player.sendMessage("");
        String translationKey = "tools." + typeId.toLowerCase() + ".name";
        MarkerData existingAtLocation = playerCastle.getLocation(location);
        if (existingAtLocation != null && existingAtLocation.isAdvancedMarker()) {
            String displayName = lang().get(translationKey);
            String existingName = lang().get(existingAtLocation.getTranslationKey());
            player.sendMessage(Component.text(lang().get("tools.advanced_tool.blocked_existing", displayName, existingName), NamedTextColor.RED));
            return;
        }
        if (typeId.equalsIgnoreCase("start") || typeId.equalsIgnoreCase("end")) {
            List<MarkerData> oldMarker = playerCastle.getMarker(typeId);
            if (!oldMarker.isEmpty()) {
                sendMarker(player, oldMarker.getFirst().getLocation(), oldMarker.getFirst().getOriginalMaterial().createBlockData());
                playerCastle.removeMarker(oldMarker.getFirst());
            }
            //WIRD NOCH GEÄNDERT DAMIT ES NCIHTH HARDCODED IST !!!!!!!!!!!!!!!!
        } else if (typeId.equalsIgnoreCase("checkpoint")) {
            MarkerData m = playerCastle.getLocation(location);
            if (m != null) {
                sendMarker(player, m.getLocation(), m.getOriginalMaterial().createBlockData());
                String displayName = lang().get(translationKey);
                player.sendActionBar(Component.text(lang().get("tools.advanced_tool.removed_detail", displayName, MarkerData.formatLocation(m.getLocation())), NamedTextColor.GOLD));
                player.sendMessage(Component.text(lang().get("tools.advanced_tool.removed_detail", displayName, MarkerData.formatLocation(m.getLocation())), NamedTextColor.GREEN));
                playerCastle.removeMarker(m);
                return;
            }
        }

        MarkerData marker = new MarkerData(this, location.clone(), typeId, translationKey);
        playerCastle.addMarker(marker);

        String displayName = lang().get(translationKey);
        player.sendActionBar(Component.text(displayName + " " + lang().get("tools.advanced_tool.placed"), NamedTextColor.GOLD));
        player.sendMessage(Component.text(lang().get("tools.advanced_tool.placed_detail", displayName, MarkerData.formatLocation(marker.getLocation())), NamedTextColor.GREEN));

        player.sendMessage("");
        revealMarkers(player, playerCastle, getTypeId());

    }

    public String getName() {
        return "Tool";
    }

    public String getTypeId() {
        return getName().toLowerCase();
    }

    public String getDisplayName() {
        String key = "tools." + getTypeId() + ".name";
        return languageManager.get(key);
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

    protected Material getDisplayMaterial(World world, MarkerData marker) {
        return world.getBlockAt(marker.getLocation()).getType();
    }

    public abstract void triggerEnter(Player player, MarkerData marker);

    public abstract void triggerExit(Player player);

    public boolean canLeftClick() {
        return false;
    }

}
