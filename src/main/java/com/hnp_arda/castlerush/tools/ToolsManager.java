package com.hnp_arda.castlerush.tools;

import com.hnp_arda.castlerush.managers.GameManager;
import com.hnp_arda.castlerush.managers.GameManager.GameState;
import com.hnp_arda.castlerush.core.PlayerCastle;
import com.hnp_arda.castlerush.tools.tools.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;

public class ToolsManager implements Listener {

    private final GameManager gameManager;
    private final ArrayList<BaseTool> TOOLS = new ArrayList<>();

    public ToolsManager(GameManager gameManager) {
        this.gameManager = gameManager;
        gameManager.getPlugin().getServer().getPluginManager().registerEvents(this, gameManager.getPlugin());
        registerTools();
    }

    private void registerTools() {
        registerTool(new StartTool(gameManager));
        registerTool(new EndTool(gameManager));
        registerTool(new CheckpointTool(gameManager));
        registerTool(new DeathzoneTool(gameManager));
        registerTool(new EffectTool(gameManager));
        registerTool(new MarkerViewerTool(gameManager));
        registerTool(new TeleporterTool(gameManager));
    }

    private void registerTool(BaseTool tool) {
        TOOLS.add(tool);
    }

    public BaseTool getTool(ItemStack item) {
        if (item == null) return null;
        List<BaseTool> t = TOOLS.stream().filter(tool -> tool.getToolItem().equals(item.getType())).toList();
        if (t.isEmpty()) return null;
        return t.getFirst();
    }

    public BaseTool getTool(String toolName) {
        if (toolName == null || toolName.isEmpty()) return null;
        try {
            return TOOLS.stream().filter(t -> t.getName().equalsIgnoreCase(toolName)).toList().getFirst();
        } catch (Exception e) {
            return null;
        }
    }

    public BaseTool getToolByTypeID(String typeID) {
        try {
            return TOOLS.stream().filter(t -> t.getTypeId().equals(typeID)).toList().getFirst();
        } catch (Exception e) {
            return null;
        }
    }

    public ArrayList<BaseTool> getTools() {
        return TOOLS;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (gameManager.getGameState() != GameState.BUILDING) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        BaseTool tool = getTool(event.getItem());
        if (tool == null || event.getItem() == null || !event.getItem().hasItemMeta()) return;
        boolean rightClick = event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR;
        boolean leftClick = event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR;
        if (!rightClick && !(leftClick && tool.canLeftClick() )) return;

        Player player = event.getPlayer();
        PlayerCastle playerCastle = gameManager.getPlayerCastle(player);
        if (playerCastle == null) return;

        event.setCancelled(true);
        tool.handleInteract(event, playerCastle);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (gameManager.getGameState() != GameState.BUILDING) return;

        PlayerInventory inv = player.getInventory();

        ItemStack prevItem = inv.getItem(event.getPreviousSlot());
        BaseTool prevTool = getTool(prevItem);
        if (prevTool != null) {

            if (event.getPlayer().isSneaking()) {
                int index = event.getNewSlot() - event.getPreviousSlot();
                if (index != 0 && prevTool.scrollEvent(player, index)) {
                    event.setCancelled(true);
                    return;
                }
            }


            PlayerCastle playerCastle = gameManager.getPlayerCastle(player);
            if (playerCastle != null) {
                prevTool.onDeselect(player, playerCastle);
            }
        }

        ItemStack newItem = inv.getItem(event.getNewSlot());
        BaseTool newTool = getTool(newItem);
        if (newTool != null) {
            assert newItem != null;
            if (newItem.hasItemMeta()) {
                PlayerCastle playerCastle = gameManager.getPlayerCastle(player);
                if (playerCastle != null) {
                    newTool.onSelect(player, playerCastle);
                }
            }
        }
    }

    public List<String> getToolsNames(boolean lowerCase) {
        return getTools().stream().map(tool -> lowerCase ? tool.getName().toLowerCase() : tool.getName()).toList();
    }
}
