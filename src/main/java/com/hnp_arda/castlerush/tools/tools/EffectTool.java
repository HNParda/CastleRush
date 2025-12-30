package com.hnp_arda.castlerush.tools.tools;

import com.hnp_arda.castlerush.core.PlayerCastle;
import com.hnp_arda.castlerush.managers.GameManager;
import com.hnp_arda.castlerush.tools.BaseZoneTool;
import com.hnp_arda.castlerush.core.Marker;
import com.hnp_arda.castlerush.tools.tools.effect.*;
import com.hnp_arda.castlerush.tools.tools.effect.Effect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
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

public class EffectTool extends BaseZoneTool implements Listener {

    private final List<Effect> EFFECTS = new ArrayList<>();
    private final Map<UUID, EffectSelection> selections = new HashMap<>();
    private final Map<UUID, EffectTool.ActiveEffect> activeEffects;

    public EffectTool(GameManager gameManager) {
        super(gameManager);
        gameManager.getPlugin().getServer().getPluginManager().registerEvents(this, gameManager.getPlugin());
        this.activeEffects = new HashMap<>();
        registerEffects();
    }

    private void registerEffects() {
        registerEffect(new SpeedEffect());
        registerEffect(new JumpboostEffect());
        registerEffect(new BreathingEffect());

        registerEffect(new NauseaEffect());
        registerEffect(new SlownessEffect());
        registerEffect(new BlindnessEffect());
        registerEffect(new SlowfallingEffect());
    }

    private void registerEffect(Effect effect) {
        EFFECTS.add(effect);
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
    protected Material getStartMaterial(Player player) {
        Effect selected = getSelectedEffect(player);
        return selected != null ? selected.getStartBlock() : Material.LIGHT_GRAY_CONCRETE;
    }

    @Override
    public void onDeselect(Player player, PlayerCastle playerCastle) {
        if (!selections.containsKey(player.getUniqueId())) return;
        hideMarkers(player, playerCastle, getTypeId());
        Location start = getZoneStart(player);
        if (start != null) player.sendBlockChange(start, start.getBlock().getBlockData());

    }

    @EventHandler
    public void handleInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(guiTitle())) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        List<Effect> good = EFFECTS.stream().filter(Effect::isGoodEffect).toList();
        List<Effect> bad = EFFECTS.stream().filter(effect -> !effect.isGoodEffect()).toList();

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
        EffectSelection selection = getSelection(player);
        if (selection == null) return;

        String data = String.format("%s;%s;%s", getTypeId(), selection.effect.getEffectName(), selection.amplifier);

        interact(player, location, playerCastle, data, result -> {
        });

        player.sendMessage(Component.text(""));

    }

    private void openEffectSelector(Player player) {
        getSelectedEffect(player);
        Inventory inv = Bukkit.createInventory(player, InventoryType.CHEST, guiTitle());
        List<Effect> good = EFFECTS.stream().filter(Effect::isGoodEffect).toList();
        List<Effect> bad = EFFECTS.stream().filter(effect -> !effect.isGoodEffect()).toList();

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
        EffectSelection sel = getSelection(player);
        Effect effect = sel.effect();
        int level = sel.amplifier();

        String effectName = getEffectLabel(effect);
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
        EffectSelection sel = getSelection(player);
        return sel.effect();
    }

    private Component guiTitle() {
        return Component.text(lang().get("tools.effect.messages.gui_title"));
    }

    private Effect getEffectByMarker(Marker marker) {
        String data = marker.getData();
        String[] dataArray = data.split(";");
        return getEffectByMarkerData(dataArray);
    }

    private Effect getEffectByMarkerData(String[] data) {
        if (!data[0].equals("effect") || data.length != 3) return null;
        String effectName = data[1];
        return EFFECTS.stream().filter(effect -> effect.getEffectName().equals(effectName)).toList().getFirst();
    }

    private ActiveEffect getActiveEffectByMarkerData(Marker marker) {
        String data = marker.getData();
        String[] dataArray = data.split(";");
        Effect effect = getEffectByMarkerData(dataArray);
        return new ActiveEffect(effect, Integer.parseInt(dataArray[2]));
    }

    private EffectSelection getSelection(Player player) {
        return selections.computeIfAbsent(player.getUniqueId(), id -> new EffectSelection(EFFECTS.getFirst(), 1));
    }

    private void adjustAmplifier(Player player, int delta) {
        EffectSelection sel = getSelection(player);
        int next = clampLevel(sel.amplifier() + delta);
        selections.put(player.getUniqueId(), new EffectSelection(sel.effect, next));
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
    }

    @Override
    public Material getDisplayMaterial(World world, Marker marker) {
        Effect selectedEffect = getEffectByMarker(marker);
        if (selectedEffect == null) return null;
        return selectedEffect.getDisplayMaterial(marker.isAir());
    }

    @Override
    public void triggerEnter(Player player, Marker marker) {
        if (player.getGameMode() != GameMode.SURVIVAL) return;

        ActiveEffect effect = getActiveEffectByMarkerData(marker);
        ActiveEffect current = activeEffects.get(player.getUniqueId());

        if (!isSameEffect(effect, current))
            applyEffect(player, effect);
    }

    @Override
    public void triggerExit(Player player) {
        ActiveEffect current = activeEffects.get(player.getUniqueId());
        if (current != null)
            clearEffect(player, current);
    }

    private void applyEffect(Player player, ActiveEffect effect) {
        activeEffects.put(player.getUniqueId(), effect);
        effect.effect().apply(player, effect.level());
    }

    private void clearEffect(Player player, ActiveEffect effect) {
        if (effect == null || effect.effect() == null) return;
        activeEffects.remove(player.getUniqueId());
        effect.effect().clear(player);
    }

    private boolean isSameEffect(ActiveEffect first, ActiveEffect second) {
        if (first == null || second == null)
            return false;

        return first.effect().getEffectName().equalsIgnoreCase(second.effect().getEffectName()) && first.level() == second.level();
    }

    @Override
    public boolean canLeftClick() {
        return true;
    }

    private record ActiveEffect(com.hnp_arda.castlerush.tools.tools.effect.Effect effect, int level) {
    }

    private record EffectSelection(Effect effect, int amplifier) {
    }
}
