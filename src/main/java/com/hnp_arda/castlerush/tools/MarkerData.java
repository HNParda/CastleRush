package com.hnp_arda.castlerush.tools;

import com.hnp_arda.castlerush.tools.effect.Effect;
import com.hnp_arda.castlerush.tools.effect.EffectRegistry;
import org.bukkit.Location;
import org.bukkit.Material;

public class MarkerData {

    private final Location location;
    private final String typeId;
    private final String translationKey;
    private Material originalMaterial;
    private Material displayMaterial;
    private String effectTypeName;
    private Effect effect;
    private int amplifier = 1;

    public MarkerData(Location location, Material originalMaterial, String typeId, String translationKey, Material displayMaterial) {
        this.location = location;
        this.originalMaterial = originalMaterial;
        this.typeId = typeId;
        this.translationKey = translationKey;
        this.displayMaterial = displayMaterial;
    }
//WIRD NOCH GEÄNDERT HARDCODED FSKadDKS
    public MarkerData(Location location, Material originalMaterial, String typeId, String translationKey, Material displayMaterial, Effect effect) {
        this(location, originalMaterial, typeId, translationKey, displayMaterial);
        setEffect(effect);
    }

    public static String formatLocation(Location loc) {
        return String.format("X:%d Y:%d Z:%d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public Location getLocation() {
        return location;
    }

    public Material getOriginalMaterial() {
        return originalMaterial;
    }

    public void setOriginalMaterial(Material newMaterial) {
        originalMaterial = newMaterial;
    }

    public String getTypeId() {
        return typeId;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public Material getDisplayMaterial() {
        return displayMaterial != null ? displayMaterial : originalMaterial;
    }

    public void setDisplayMaterial(Material displayMaterial) {
        this.displayMaterial = displayMaterial;
    }

    public Effect getEffect() {
        if (effect != null) return effect;
        if (effectTypeName != null) {
            effect = EffectRegistry.byId(effectTypeName);
        }
        return effect;
    }

    public void setEffect(Effect effect) {
        this.effect = effect;
        this.effectTypeName = effect != null ? effect.getEffectName() : null;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public void setAmplifier(int amplifier) {
        this.amplifier = amplifier;
    }

}
