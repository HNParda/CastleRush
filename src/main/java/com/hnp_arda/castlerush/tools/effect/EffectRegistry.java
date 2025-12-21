package com.hnp_arda.castlerush.tools.effect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EffectRegistry {

    private static final Map<String, Effect> EFFECTS = new LinkedHashMap<>();

    static {
        register(new SpeedEffect());
        register(new JumpboostEffect());
        register(new BreathingEffect());

        register(new NauseaEffect());
        register(new SlownessEffect());
        register(new BlindnessEffect());
        register(new SlowfallingEffect());
    }

    private EffectRegistry() {
    }

    private static void register(Effect effect) {
        EFFECTS.put(effect.getEffectName().toLowerCase(Locale.ROOT), effect);
    }

    public static List<Effect> all() {
        return new ArrayList<>(EFFECTS.values());
    }

    public static Effect firstGood() {
        return EFFECTS.values().stream().filter(Effect::isGoodEffect).findFirst().orElseGet(() -> EFFECTS.values().stream().findFirst().orElse(null));
    }

    public static Effect byId(String id) {
        if (id == null) return null;
        return EFFECTS.get(id.toLowerCase(Locale.ROOT));
    }
}
