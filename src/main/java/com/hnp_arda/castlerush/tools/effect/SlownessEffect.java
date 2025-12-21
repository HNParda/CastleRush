package com.hnp_arda.castlerush.tools.effect;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public class SlownessEffect extends Effect {
    @Override
    public String getEffectName() {
        return "slowness";
    }

    @Override
    public Material getStartBlock() {
        return Material.ORANGE_CONCRETE;
    }

    @Override
    public Material getSolidBlock() {
        return Material.ORANGE_WOOL;
    }

    @Override
    public Material getAirBlock() {
        return Material.ORANGE_STAINED_GLASS;
    }

    @Override
    public Material getIcon() {
        return Material.SOUL_SAND;
    }

    @Override
    public PotionEffectType getPotionType() {
        return PotionEffectType.SLOWNESS;
    }

    @Override
    public boolean isGoodEffect() {
        return false;
    }

    @Override
    public NamedTextColor getColor() {
        return NamedTextColor.GOLD;
    }
}
