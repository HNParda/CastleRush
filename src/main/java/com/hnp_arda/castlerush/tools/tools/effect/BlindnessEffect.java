package com.hnp_arda.castlerush.tools.tools.effect;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public class BlindnessEffect extends Effect {
    @Override
    public String getEffectName() {
        return "blindness";
    }

    @Override
    public Material getStartBlock() {
        return Material.BLACK_CONCRETE;
    }

    @Override
    public Material getSolidBlock() {
        return Material.BLACK_WOOL;
    }

    @Override
    public Material getAirBlock() {
        return Material.BLACK_STAINED_GLASS;
    }

    @Override
    public Material getIcon() {
        return Material.COAL;
    }

    @Override
    public PotionEffectType getPotionType() {
        return PotionEffectType.BLINDNESS;
    }

    @Override
    public boolean isGoodEffect() {
        return false;
    }

    @Override
    public NamedTextColor getColor() {
        return NamedTextColor.DARK_GRAY;
    }
}
