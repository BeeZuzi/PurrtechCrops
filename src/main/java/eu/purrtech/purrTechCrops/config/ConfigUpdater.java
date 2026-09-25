package eu.purrtech.purrTechCrops.config;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds keys introduced by newer plugin versions to an existing config.yml, keeping the user's values.
 */
public final class ConfigUpdater {

    static final String VERSION_KEY = "config-version";

    // Entries of the crops section are user choices; a removed crop must not come back on update.
    private static final String CROPS_KEY = "crops";

    private ConfigUpdater() {
    }

    /**
     * Copies missing keys from the config's defaults into the config itself.
     *
     * @return the added keys; empty if the config was already complete and up to date
     */
    public static List<String> update(Configuration config) {
        Configuration defaults = config.getDefaults();
        if (defaults == null) {
            return List.of();
        }
        boolean hasCrops = config.isSet(CROPS_KEY);
        List<String> added = new ArrayList<>();
        for (String key : defaults.getKeys(true)) {
            if (defaults.get(key) instanceof ConfigurationSection || config.isSet(key)) {
                continue;
            }
            if (hasCrops && key.startsWith(CROPS_KEY + ".")) {
                continue;
            }
            config.set(key, defaults.get(key));
            added.add(key);
        }

        int latestVersion = defaults.getInt(VERSION_KEY);
        if (config.getInt(VERSION_KEY) != latestVersion) {
            config.set(VERSION_KEY, latestVersion);
            if (!added.contains(VERSION_KEY)) {
                added.add(VERSION_KEY);
            }
        }
        return added;
    }
}
