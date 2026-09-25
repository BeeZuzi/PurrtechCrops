package eu.purrtech.purrTechCrops.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigUpdaterTest {

    private YamlConfiguration defaults;

    @BeforeEach
    void setUp() throws Exception {
        defaults = PluginConfigTest.bundledConfig();
    }

    @Test
    void completeConfigIsNotChanged() throws Exception {
        YamlConfiguration config = PluginConfigTest.bundledConfig();
        config.setDefaults(defaults);

        assertTrue(ConfigUpdater.update(config).isEmpty());
    }

    @Test
    void missingKeysAreAddedAndUserValuesKept() throws Exception {
        YamlConfiguration config = PluginConfigTest.bundledConfig();
        config.set("harvest.drop-mode", "INVENTORY");
        config.set("effects", null);
        config.setDefaults(defaults);

        List<String> added = ConfigUpdater.update(config);

        assertEquals(List.of("effects.swing-hand", "effects.break-effect"), added);
        assertTrue(config.isSet("effects.swing-hand"));
        assertEquals("INVENTORY", config.getString("harvest.drop-mode"));
    }

    @Test
    void removedCropIsNotAddedBack() throws Exception {
        YamlConfiguration config = PluginConfigTest.bundledConfig();
        config.set("crops.COCOA", null);
        config.setDefaults(defaults);

        ConfigUpdater.update(config);

        assertFalse(config.isSet("crops.COCOA"));
    }

    @Test
    void missingCropsSectionIsRestored() throws Exception {
        YamlConfiguration config = PluginConfigTest.bundledConfig();
        config.set("crops", null);
        config.setDefaults(defaults);

        ConfigUpdater.update(config);

        assertEquals("COCOA_BEANS", config.getString("crops.COCOA"));
        assertEquals(6, config.getConfigurationSection("crops").getKeys(false).size());
    }

    @Test
    void oldConfigVersionIsBumped() throws Exception {
        YamlConfiguration config = PluginConfigTest.bundledConfig();
        config.set(ConfigUpdater.VERSION_KEY, 0);
        config.setDefaults(defaults);

        List<String> added = ConfigUpdater.update(config);

        assertEquals(List.of(ConfigUpdater.VERSION_KEY), added);
        assertEquals(defaults.getInt(ConfigUpdater.VERSION_KEY), config.getInt(ConfigUpdater.VERSION_KEY));
    }
}
