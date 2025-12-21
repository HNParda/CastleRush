package com.hnp_arda.castlerush.tools.effect;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public class BreathingEffect extends Effect {
    @Override
    public String getEffectName() {
        return "water_breathing";
    }

    @Override
    public Material getStartBlock() {
        return Material.BLUE_CONCRETE;
    }

    @Override
    public Material getSolidBlock() {
        return Material.BLUE_WOOL;
    }

    @Override
    public Material getAirBlock() {
        return Material.BLUE_STAINED_GLASS;
    }

    @Override
    public Material getIcon() {
        return Material.TURTLE_HELMET;
    }

    @Override
    public PotionEffectType getPotionType() {
        return PotionEffectType.WATER_BREATHING;
    }

    @Override
    public NamedTextColor getColor() {
        return NamedTextColor.DARK_BLUE;
    }
}
