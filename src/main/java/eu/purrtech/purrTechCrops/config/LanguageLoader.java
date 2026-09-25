package eu.purrtech.purrTechCrops.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Loads language files from {@code plugins/PurrTechCrops/lang/}. Bundled languages are copied there on first
 * start; keys missing from a file fall back to the bundled version, so old files keep working after updates.
 */
public final class LanguageLoader {

    public static final String DEFAULT_LANGUAGE = "en";
    private static final List<String> BUNDLED = List.of("en", "cs");
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]+");

    private final Plugin plugin;

    public LanguageLoader(Plugin plugin) {
        this.plugin = plugin;
    }

    public Messages load(String language) {
        for (String bundled : BUNDLED) {
            if (!file(bundled).exists()) {
                plugin.saveResource(resourcePath(bundled), false);
            }
        }

        if (!VALID_NAME.matcher(language).matches() || !file(language).exists()) {
            plugin.getLogger().warning("language: '" + language + "' not found in lang/, using "
                    + DEFAULT_LANGUAGE + ".");
            language = DEFAULT_LANGUAGE;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file(language));
        String defaultsLanguage = BUNDLED.contains(language) ? language : DEFAULT_LANGUAGE;
        try (InputStream defaults = plugin.getResource(resourcePath(defaultsLanguage))) {
            if (defaults != null) {
                yaml.setDefaults(YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaults, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not read bundled " + resourcePath(defaultsLanguage) + ": " + e);
        }
        return Messages.load(yaml);
    }

    private File file(String language) {
        return new File(plugin.getDataFolder(), resourcePath(language));
    }

    private static String resourcePath(String language) {
        return "lang/" + language + ".yml";
    }
}
