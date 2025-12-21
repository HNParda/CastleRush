package com.hnp_arda.castlerush.tools.effect;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public abstract class Effect {

    public abstract String getEffectName();

    public abstract Material getStartBlock();

    public abstract Material getSolidBlock();

    public abstract Material getAirBlock();

    public abstract Material getIcon();

    public abstract PotionEffectType getPotionType();

    public boolean isGoodEffect() {
        return true;
    }

    public NamedTextColor getColor() {
        return NamedTextColor.LIGHT_PURPLE;
    }

    public PotionEffect asPotionEffect(int level) {
        PotionEffectType type = getPotionType();
        if (type == null) return null;
        int amp = Math.max(0, level - 1);
        return new PotionEffect(type, PotionEffect.INFINITE_DURATION, amp, true, false, true);
    }

    public void apply(Player player, int level) {
        PotionEffect effect = asPotionEffect(level);
        if (effect != null) {
            player.addPotionEffect(effect);
        }
    }

    public void clear(Player player) {
        PotionEffectType type = getPotionType();
        if (type != null) {
            player.removePotionEffect(type);
        }
    }

    public Material getDisplayMaterial(boolean air) {
        return air ? getAirBlock() : getSolidBlock();
    }
}
