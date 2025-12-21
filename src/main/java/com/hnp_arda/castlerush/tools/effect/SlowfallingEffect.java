package com.hnp_arda.castlerush.tools.effect;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public class SlowfallingEffect extends Effect {
    @Override
    public String getEffectName() {
        return "slow_falling";
    }

    @Override
    public Material getStartBlock() {
        return Material.WHITE_CONCRETE;
    }

    @Override
    public Material getSolidBlock() {
        return Material.WHITE_WOOL;
    }

    @Override
    public Material getAirBlock() {
        return Material.WHITE_STAINED_GLASS;
    }

    @Override
    public Material getIcon() {
        return Material.FEATHER;
    }

    @Override
    public PotionEffectType getPotionType() {
        return PotionEffectType.SLOW_FALLING;
    }

    @Override
    public boolean isGoodEffect() {
        return false;
    }

    @Override
    public NamedTextColor getColor() {
        return NamedTextColor.WHITE;
    }
}
