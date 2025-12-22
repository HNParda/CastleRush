package com.hnp_arda.castlerush.tools.tools.effect;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public class JumpboostEffect extends Effect {
    @Override
    public String getEffectName() {
        return "jump_boost";
    }

    @Override
    public Material getStartBlock() {
        return Material.LIME_CONCRETE;
    }

    @Override
    public Material getSolidBlock() {
        return Material.LIME_WOOL;
    }

    @Override
    public Material getAirBlock() {
        return Material.LIME_STAINED_GLASS;
    }

    @Override
    public Material getIcon() {
        return Material.RABBIT_FOOT;
    }

    @Override
    public PotionEffectType getPotionType() {
        return PotionEffectType.JUMP_BOOST;
    }

    @Override
    public NamedTextColor getColor() {
        return NamedTextColor.GREEN;
    }
}
