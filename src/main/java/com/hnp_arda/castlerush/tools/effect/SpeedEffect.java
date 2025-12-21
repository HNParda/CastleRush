package com.hnp_arda.castlerush.tools.effect;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public class SpeedEffect extends Effect {
    @Override
    public String getEffectName() {
        return "speed";
    }

    @Override
    public Material getStartBlock() {
        return Material.LIGHT_BLUE_CONCRETE;
    }

    @Override
    public Material getSolidBlock() {
        return Material.LIGHT_BLUE_WOOL;
    }

    @Override
    public Material getAirBlock() {
        return Material.LIGHT_BLUE_STAINED_GLASS;
    }

    @Override
    public Material getIcon() {
        return Material.SUGAR;
    }

    @Override
    public PotionEffectType getPotionType() {
        return PotionEffectType.SPEED;
    }

    @Override
    public NamedTextColor getColor() {
        return NamedTextColor.AQUA;
    }
}
