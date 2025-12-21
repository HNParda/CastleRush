package com.hnp_arda.castlerush.tools.effect;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public class NauseaEffect extends Effect {
    @Override
    public String getEffectName() {
        return "nausea";
    }

    @Override
    public Material getStartBlock() {
        return Material.YELLOW_CONCRETE;
    }

    @Override
    public Material getSolidBlock() {
        return Material.YELLOW_WOOL;
    }

    @Override
    public Material getAirBlock() {
        return Material.YELLOW_STAINED_GLASS;
    }

    @Override
    public Material getIcon() {
        return Material.NAUTILUS_SHELL;
    }

    @Override
    public PotionEffectType getPotionType() {
        return PotionEffectType.NAUSEA;
    }

    @Override
    public boolean isGoodEffect() {
        return false;
    }

    @Override
    public NamedTextColor getColor() {
        return NamedTextColor.YELLOW;
    }
}
