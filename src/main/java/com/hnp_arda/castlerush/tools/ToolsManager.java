package com.hnp_arda.castlerush.tools;

import com.hnp_arda.castlerush.GameManager;
import com.hnp_arda.castlerush.GameManager.GameState;
import com.hnp_arda.castlerush.PlayerCastle;
import com.hnp_arda.castlerush.tools.effect.EffectTool;
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
    private final ArrayList<Tool> tools = new ArrayList<>();

    public ToolsManager(GameManager gameManager) {
        this.gameManager = gameManager;
        gameManager.getPlugin().getServer().getPluginManager().registerEvents(this, gameManager.getPlugin());
        registerTools();
    }

    private void registerTools() {
        addTool(new StartTool(gameManager));
        addTool(new EndTool(gameManager));
        addTool(new CheckpointTool(gameManager));
        addTool(new DeathzoneTool(gameManager));
        addTool(new EffectTool(gameManager));
        addTool(new MarkerViewerTool(gameManager));
    }

    private void addTool(Tool tool) {
        tools.add(tool);
    }

    public Tool getTool(ItemStack item) {
        if (item == null) return null;
        List<Tool> t = tools.stream().filter(tool -> tool.getToolItem().equals(item.getType())).toList();
        if (t.isEmpty()) return null;
        return t.getFirst();
    }

    public Tool getTool(String toolName) {
        if (toolName == null || toolName.isEmpty()) return null;
        try {
            return tools.stream().filter(t -> t.getName().equalsIgnoreCase(toolName)).toList().getFirst();
        } catch (Exception e) {
            return null;
        }
    }

    public ArrayList<Tool> getTools() {
        return tools;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (gameManager.getGameState() != GameState.BUILDING) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Tool tool = getTool(event.getItem());
        if (tool == null || event.getItem() == null || !event.getItem().hasItemMeta()) return;
        boolean rightClick = event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR;
        boolean leftClick = event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR;
        if (!rightClick && !(tool instanceof EffectTool && leftClick)) return;
//WIRD NOCH UMGEBAUT DAMIT ES NICHT HARDCOCDED IST
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
        Tool prevTool = getTool(prevItem);
        if (prevTool != null) {
            PlayerCastle playerCastle = gameManager.getPlayerCastle(player);
            if (playerCastle != null) {
                prevTool.onDeselect(player, playerCastle);
            }
        }

        ItemStack newItem = inv.getItem(event.getNewSlot());
        Tool newTool = getTool(newItem);
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
