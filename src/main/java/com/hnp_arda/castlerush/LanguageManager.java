package com.hnp_arda.castlerush;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LanguageManager {

    private final JavaPlugin plugin;
    private final Map<String, YamlConfiguration> languages;
    private String language = "en";

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.languages = new HashMap<>();
        loadLanguages();
    }

    private void loadLanguages() {
        File languageDir = new File(plugin.getDataFolder(), "languages");
        if (!languageDir.exists()) {
            languageDir.mkdirs();
        }

        for (String code : List.of("de", "en", "tr")) {
            String resourcePath = "languages/" + code + ".yml";
            File targetFile = new File(languageDir, code + ".yml");

            if (!targetFile.exists()) {
                plugin.saveResource(resourcePath, false);
            }

            YamlConfiguration config = new YamlConfiguration();

            try {
                InputStreamReader reader = new InputStreamReader(new FileInputStream(targetFile), StandardCharsets.UTF_8);
                config.load(reader);
                languages.put(code, config);
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException(e);
            }


            //YamlConfiguration config = YamlConfiguration.loadConfiguration(targetFile);
            //  languages.put(code, config);
        }
    }

    public String get(String key, Object... args) {
        String languageCode = parseLanguage(language);
        YamlConfiguration config = languages.get(languageCode);

        String message = config.getString(key, languages.get("en").getString(key, key));

        if (args.length > 0) {
            message = String.format(message, args);
        }

        return message;
    }

    public boolean setLanguage(String languageCode) {
        String parsedCode = parseLanguage(languageCode);
        if (!languages.containsKey(parsedCode)) {
            return false;
        }
        language = parsedCode;
        return true;
    }

    public List<String> getAvailableLanguages() {
        List<String> available = new ArrayList<>(languages.keySet());
        available.sort(String::compareTo);
        return available;
    }

    private String parseLanguage(String locale) {
        if (locale == null || locale.isEmpty()) {
            return "en";
        }

        String[] parts = locale.split("_");
        return parts[0].toLowerCase(Locale.ROOT);
    }
}
