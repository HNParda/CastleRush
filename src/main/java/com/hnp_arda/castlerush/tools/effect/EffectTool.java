package com.hnp_arda.castlerush.tools.effect;

import com.hnp_arda.castlerush.GameManager;
import com.hnp_arda.castlerush.PlayerCastle;
import com.hnp_arda.castlerush.tools.AdvancedTool;
import com.hnp_arda.castlerush.tools.MarkerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static com.hnp_arda.castlerush.tools.MarkerData.formatLocation;

public class EffectTool extends AdvancedTool implements Listener {

    private final List<Effect> effectTypes = new ArrayList<>(EffectRegistry.all());
    private final Map<UUID, EffectSelection> selections = new HashMap<>();
    private final Map<UUID, EffectTool.ActiveEffect> activeEffects;

    public EffectTool(GameManager gameManager) {
        super(gameManager);
        gameManager.getPlugin().getServer().getPluginManager().registerEvents(this, gameManager.getPlugin());
        this.activeEffects = new HashMap<>();

    }

    @Override
    public String getName() {
        return "Effect";
    }

    @Override
    public Material getToolItem() {
        return Material.GOLDEN_SWORD;
    }

    @Override
    public void handleInteract(PlayerInteractEvent event, PlayerCastle playerCastle) {
        Player player = event.getPlayer();

        if (selections.containsKey(player.getUniqueId()) && (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
            handleSelfToggle(player);
            return;
        }

        if (player.isSneaking()) {
            openEffectSelector(player);
            return;
        }

        if (!selections.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text(lang().get("tools.effect.messages.not_selected"), NamedTextColor.RED));
            return;
        }

        if (event.getClickedBlock() == null) return;
        handleEffectZoneClick(player, gameManager.getPlayerCastle(player), event.getClickedBlock().getLocation());
    }

    @Override
    public void onSelect(Player player, PlayerCastle playerCastle) {
        if (!selections.containsKey(player.getUniqueId())) return;
        Effect selected = getSelectedEffect(player);
        updateToolNameInHand(player);
        if (playerCastle.hasEffectZoneStart()) {
            Material previewMat = selected != null ? selected.getStartBlock() : Material.LIGHT_GRAY_CONCRETE;
            sendMarker(player, playerCastle.getEffectZoneStart().clone(), previewMat.createBlockData());
            player.sendActionBar(Component.text(lang().get("tools.advanced_tool.action_end", lang().get("tools.effect.name")), NamedTextColor.GOLD));
        }
        revealMarkers(player, playerCastle, getTypeId());
    }

    @Override
    public void onDeselect(Player player, PlayerCastle playerCastle) {
        if (!selections.containsKey(player.getUniqueId())) return;
        hideMarkers(player, playerCastle, getTypeId());
        if (playerCastle.hasEffectZoneStart()) {
            Location start = playerCastle.getEffectZoneStart();
            player.sendBlockChange(start, start.getBlock().getBlockData());
        }
    }

    @EventHandler
    public void onEffectSelect(InventoryClickEvent event) {
        handleInventoryClick(event);
    }

    public void handleInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(guiTitle())) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        getSelectedEffect(player);
        List<Effect> good = effectTypes.stream().filter(Effect::isGoodEffect).toList();
        List<Effect> bad = effectTypes.stream().filter(effect -> !effect.isGoodEffect()).toList();

        int base = event.getInventory().getSize() - 9;
        if (slot == base + 3) {
            adjustAmplifier(player, -1);
            updateControlBar(event.getInventory(), player);
            updateToolNameInHand(player);
            return;
        } else if (slot == base + 5) {
            adjustAmplifier(player, 1);
            updateControlBar(event.getInventory(), player);
            updateToolNameInHand(player);
            return;
        }
        Effect effect = findEffectBySlot(slot, good, bad);
        if (effect == null) {
            updateControlBar(event.getInventory(), player);
            return;
        }

        EffectSelection prev = getSelection(player);
        int amp = 1;
        if (prev != null && prev.effect() == effect) {
            amp = prev.amplifier();
        }
        selections.put(player.getUniqueId(), new EffectSelection(effect, clampLevel(amp)));
        player.sendMessage(Component.text(lang().get("tools.effect.messages.selected", getEffectLabel(effect)), effect.getColor()));
        updateControlBar(event.getInventory(), player);
        updateToolNameInHand(player);
    }

    private void handleEffectZoneClick(Player player, PlayerCastle playerCastle, Location location) {
        if (!selections.containsKey(player.getUniqueId())) return;
        Effect selectedEffect = getSelectedEffect(player);
        if (selectedEffect == null) return;

        if (!playerCastle.hasEffectZoneStart()) {
            playerCastle.setEffectZoneStart(location.clone());
            sendMarker(player, location.clone(), selectedEffect.getStartBlock().createBlockData());
            player.sendMessage(Component.text(lang().get("tools.advanced_tool.start", formatLocation(location), getEffectLabel(selectedEffect)), NamedTextColor.LIGHT_PURPLE));
            player.sendActionBar(Component.text(lang().get("tools.advanced_tool.action_end", lang().get("tools.effect.name")), NamedTextColor.GOLD));
            return;
        }

        Location start = playerCastle.getEffectZoneStart();
        Location end = location.clone();

        boolean startInZone = playerCastle.isEffectBlock(start);
        boolean endInZone = playerCastle.isEffectBlock(end);

        List<Location> regionBlocks = getBlocksBetween(start, end);

        RegionToggleResult result = toggleRegionMarkers(player, playerCastle, regionBlocks, getTypeId(), startInZone && endInZone, loc -> {
            Material original = loc.getBlock().getType();
            //Material display = selectedEffect.getDisplayMaterial(original == Material.AIR || original.isAir());
            EffectSelection selection = getSelection(player);
            int amplifier = selection != null ? clampLevel(selection.amplifier()) : 1;
            String advancedToolData = String.format("effect;%s;%s", selectedEffect.getEffectName(), amplifier);
            return new MarkerData(this, loc.clone(), original, getTypeId(), "tools.effect.name", advancedToolData);
        }/*, (marker) -> {

            EffectSelection selection = getSelection(player);
            int amplifier = selection != null ? clampLevel(selection.amplifier()) : 1;
            String advancedToolData = String.format("effect;%s;%s", selectedEffect.getEffectName(), amplifier);
            marker.setAdvancedToolData(advancedToolData);
            // marker.setDisplayMaterial(selectedEffect.getDisplayMaterial(original == Material.AIR || original.isAir()));
        }*/);

        player.sendMessage(Component.text(""));
        if (result.removedMode()) {
            player.sendMessage(Component.text(lang().get("tools.advanced_tool.removed", result.removedCount(), getEffectLabel(selectedEffect)), NamedTextColor.YELLOW));
            player.sendMessage(Component.text(lang().get("tools.advanced_tool.total", playerCastle.getMarker(getTypeId()).size()), NamedTextColor.GRAY));
            player.sendMessage(Component.text(""));
            player.sendMessage(Component.text(""));
        } else {
            RegionChangeResult change = result.change();
            player.sendMessage(Component.text(lang().get("tools.advanced_tool.end", lang().get("tools.effect.name"), formatLocation(location), getEffectLabel(selectedEffect)), NamedTextColor.LIGHT_PURPLE));
            player.sendMessage(Component.text(lang().get("tools.advanced_tool.added", change.added(), getEffectLabel(selectedEffect)), NamedTextColor.LIGHT_PURPLE));
            player.sendMessage(Component.text(lang().get("tools.advanced_tool.total", playerCastle.getMarker(getTypeId()).size()), NamedTextColor.GRAY));

            if (change.replaced() > 0) {
                player.sendMessage(Component.text(""));
                player.sendMessage(Component.text(lang().get("tools.advanced_tool.replaced_total", change.replaced()), NamedTextColor.YELLOW));
                player.sendMessage(Component.text(lang().get("tools.advanced_tool.replaced_list", String.join(", ", change.replacedTypes())), NamedTextColor.GRAY));
            }

            player.sendMessage(Component.text(""));
        }

        playerCastle.setEffectZoneStart(null);
    }

    private void openEffectSelector(Player player) {
        if (effectTypes.isEmpty()) return;
        getSelectedEffect(player);
        Inventory inv = Bukkit.createInventory(player, InventoryType.CHEST, guiTitle());
        List<Effect> good = effectTypes.stream().filter(Effect::isGoodEffect).toList();
        List<Effect> bad = effectTypes.stream().filter(effect -> !effect.isGoodEffect()).toList();

        List<Integer> goodSlots = computeEffectSlots(good.size(), 0);
        for (int i = 0; i < good.size(); i++) {
            inv.setItem(goodSlots.get(i), createEffectIcon(good.get(i)));
        }

        List<Integer> badSlots = computeEffectSlots(bad.size(), 9);
        for (int i = 0; i < bad.size(); i++) {
            inv.setItem(badSlots.get(i), createEffectIcon(bad.get(i)));
        }
        int base = inv.getSize() - 9;
        inv.setItem(base + 3, createControlIcon(lang().get("tools.effect.effect_control.level_down")));
        inv.setItem(base + 4, createLevelIcon(player));
        inv.setItem(base + 5, createControlIcon(lang().get("tools.effect.effect_control.level_up")));
        player.openInventory(inv);
    }

    private ItemStack createEffectIcon(Effect effect) {
        ItemStack item = new ItemStack(effect.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(getEffectLabel(effect), effect.getColor()));
        item.setItemMeta(meta);
        return item;
    }

    private String getEffectLabel(Effect effect) {
        return lang().get("tools.effect.effects." + effect.getEffectName());
    }

    private List<Integer> computeEffectSlots(int count, int offset) {
        List<Integer> slots = new ArrayList<>();
        if (count <= 0) return slots;

        int start, step = 2;
        if (count % 2 == 1) {
            start = 4 - ((count - 1) / 2) * step;
        } else {
            int half = count / 2;
            start = 5 - (half) * step;
        }

        for (int i = 0; i < count; i++) {
            int pos = start + i * step;
            slots.add(pos + offset);
        }
        return slots;
    }

    private void updateToolNameInHand(Player player) {
        Effect effect = getSelectedEffect(player);
        if (effect == null) return;

        EffectSelection selection = selections.get(player.getUniqueId());
        String effectName = getEffectLabel(effect);
        int level = selection != null ? selection.amplifier() : 1;
        Component name = Component.text(lang().get("tools.effect.name") + " ", NamedTextColor.YELLOW).append(Component.text("(" + effectName + " " + level + ")", effect.getColor()));
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != getToolItem()) return;
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        player.getInventory().setItemInMainHand(item);
    }

    private Effect getSelectedEffect(Player player) {
        Effect fallback = EffectRegistry.firstGood();
        if (fallback == null && !effectTypes.isEmpty()) {
            fallback = effectTypes.getFirst();
        }
        Effect finalFallback = fallback;
        EffectSelection sel = selections.computeIfAbsent(player.getUniqueId(), id -> new EffectSelection(finalFallback, 1));
        return sel.effect();
    }
/*
    @Override
    protected Material resolveDisplayMaterial(MarkerData marker) {
        if (getTypeId().equalsIgnoreCase(marker.getTypeId()) && marker.isAdvancedMarker()) {
            Effect effect = getEffectByMarkerData(marker);
            if (effect == null) return null;
            return effect.getDisplayMaterial(marker.getOriginalMaterial() == Material.AIR || marker.getOriginalMaterial().isAir());
        } return super.resolveDisplayMaterial(marker);
    }*/

    private Component guiTitle() {
        return Component.text(lang().get("tools.effect.messages.gui_title"));
    }

    private Effect getEffectByMarkerData(MarkerData marker) {
        if (!marker.isAdvancedMarker()) return null;
        String advancedToolData = marker.getAdvancedToolData();
        String[] data = advancedToolData.split(";");
        if (data[0].equals("effect") || data.length != 3) return null;
        String effectName = data[1];
        return effectTypes.stream().filter(effect -> effect.getEffectName().equals(effectName)).toList().getFirst();
    }

    private ActiveEffect getActiveEffectByMarkerData(MarkerData marker) {
        if (!marker.isAdvancedMarker()) return null;
        String advancedToolData = marker.getAdvancedToolData();
        String[] data = advancedToolData.split(";");
        if (data[0].equals("effect") || data.length != 3) return null;
        Effect effect = effectTypes.stream().filter(e -> e.getEffectName().equals(data[1])).toList().getFirst();
        return new ActiveEffect(effect, Integer.parseInt(data[2]));
    }

    private EffectSelection getSelection(Player player) {
        return selections.get(player.getUniqueId());
    }

    private void adjustAmplifier(Player player, int delta) {
        Effect selected = getSelectedEffect(player);
        EffectSelection sel = getSelection(player);
        if (sel == null) {
            sel = new EffectSelection(selected, 1);
        }
        int next = clampLevel(sel.amplifier() + delta);
        selections.put(player.getUniqueId(), new EffectSelection(selected, next));
    }

    private void updateControlBar(Inventory inv, Player player) {
        int base = inv.getSize() - 9;
        inv.setItem(base + 4, createLevelIcon(player));
    }

    private ItemStack createControlIcon(String name) {
        ItemStack item = new ItemStack(Material.OAK_BUTTON);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLevelIcon(Player player) {
        EffectSelection sel = getSelection(player);
        int level = clampLevel(sel != null ? sel.amplifier() : 1);
        ItemStack item = new ItemStack(Material.GLOWSTONE_DUST, Math.max(1, Math.min(64, level)));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(lang().get("tools.effect.effect_control.level", level), NamedTextColor.GOLD));
        item.setItemMeta(meta);
        return item;
    }

    private int clampLevel(int level) {
        return Math.max(1, Math.min(10, level));
    }

    private Effect findEffectBySlot(int slot, List<Effect> good, List<Effect> bad) {
        List<Integer> goodSlots = computeEffectSlots(good.size(), 0);
        for (int i = 0; i < good.size(); i++) {
            if (goodSlots.get(i) == slot) return good.get(i);
        }
        List<Integer> badSlots = computeEffectSlots(bad.size(), 9);
        for (int i = 0; i < bad.size(); i++) {
            if (badSlots.get(i) == slot) return bad.get(i);
        }
        return null;
    }

    private void handleSelfToggle(Player player) {
        Effect selected = getSelectedEffect(player);
        EffectSelection sel = getSelection(player);
        int level = clampLevel(sel != null ? sel.amplifier() : 1);
        if (selected == null || selected.getPotionType() == null) return;

        boolean had = player.hasPotionEffect(selected.getPotionType());
        if (had) {
            selected.clear(player);
            player.sendActionBar(Component.text(lang().get("tools.effect.messages.cleared", getEffectLabel(selected)), NamedTextColor.YELLOW));
        } else {
            selected.apply(player, level);
            player.sendActionBar(Component.text(lang().get("tools.effect.messages.applied", getEffectLabel(selected), level), selected.getColor()));
        }
        updateToolNameInHand(player);
    }

    @Override
    protected Material getDisplayMaterial(World world, MarkerData marker) {
        Effect selectedEffect = getEffectByMarkerData(marker);
        Material original = super.getDisplayMaterial(world, marker);
        if (selectedEffect == null) return null;
        return selectedEffect.getDisplayMaterial(original == Material.AIR || original.isAir());
    }

    @Override
    public void triggerEnter(Player player, MarkerData marker) {

        ActiveEffect effect = getActiveEffectByMarkerData(marker);
        ActiveEffect current = activeEffects.get(player.getUniqueId());

        if (effect == null) {
            if (current != null) {
                clearEffect(player, current);
                activeEffects.remove(player.getUniqueId());
            }
            return;
        }
        if (!isSameEffect(effect, current)) {
            if (current != null) {
                clearEffect(player, current);
            }
            applyEffect(player, effect);
            activeEffects.put(player.getUniqueId(), effect);
        }
    }

    @Override
    public void triggerExit(Player player) {

        ActiveEffect current = activeEffects.get(player.getUniqueId());

        if (current != null) {
            clearEffect(player, current);
            activeEffects.remove(player.getUniqueId());
        }

    }

    private void applyEffect(Player player, ActiveEffect effect) {
        if (effect == null || effect.effect() == null) return;
        effect.effect().apply(player, effect.level());
    }

    private void clearEffect(Player player, ActiveEffect effect) {
        if (effect == null || effect.effect() == null) return;
        effect.effect().clear(player);
    }

    private boolean isSameEffect(ActiveEffect first, ActiveEffect second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.effect().getEffectName().equalsIgnoreCase(second.effect().getEffectName()) && first.level() == second.level();
    }

    private record ActiveEffect(com.hnp_arda.castlerush.tools.effect.Effect effect, int level) {
    }

    private record EffectSelection(Effect effect, int amplifier) {
    }
}
