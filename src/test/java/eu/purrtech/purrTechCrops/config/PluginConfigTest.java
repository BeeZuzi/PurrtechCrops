package eu.purrtech.purrTechCrops.config;

import eu.purrtech.purrTechCrops.harvest.DropMode;
import eu.purrtech.purrTechCrops.harvest.ReplantFallback;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginConfigTest {

    private static final Logger LOGGER = Logger.getLogger("PluginConfigTest");
    private static final Messages NO_MESSAGES = Messages.load(new YamlConfiguration());

    @BeforeEach
    void setUp() {
        // Material#createBlockData needs a running server.
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void bundledConfigLoadsAllCropsWithDefaults() throws Exception {
        PluginConfig config = PluginConfig.load(bundledConfig(), NO_MESSAGES, LOGGER);

        assertEquals(6, config.crops().size());
        assertEquals(Material.COCOA_BEANS, config.crops().find(Material.COCOA).orElseThrow().replantItem());
        assertEquals(DropMode.GROUND, config.dropMode());
        assertEquals(ReplantFallback.FREE, config.replantFallback());
        assertFalse(config.strictProtection());
        assertTrue(config.applyFortune());
    }

    @Test
    void invalidCropsAreSkipped() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("crops.WHEAT", "WHEAT_SEEDS");
        yaml.set("crops.STONE", "COBBLESTONE");          // not a crop
        yaml.set("crops.NOT_A_BLOCK", "WHEAT_SEEDS");    // unknown material
        yaml.set("crops.CARROTS", "NOT_AN_ITEM");        // unknown replant item
        yaml.set("crops.PITCHER_CROP", "PITCHER_POD");   // two blocks tall

        PluginConfig config = PluginConfig.load(yaml, NO_MESSAGES, LOGGER);

        assertEquals(1, config.crops().size());
        assertTrue(config.crops().find(Material.WHEAT).isPresent());
    }

    @Test
    void invalidEnumFallsBackToDefault() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("harvest.drop-mode", "somewhere");
        yaml.set("harvest.replant-fallback", "skip");

        PluginConfig config = PluginConfig.load(yaml, NO_MESSAGES, LOGGER);

        assertEquals(DropMode.GROUND, config.dropMode());
        assertEquals(ReplantFallback.SKIP, config.replantFallback());
    }

    @Test
    void missingCropsSectionUsesDefaultCrops() {
        PluginConfig config = PluginConfig.load(new YamlConfiguration(), NO_MESSAGES, LOGGER);

        assertEquals(6, config.crops().size());
    }

    static YamlConfiguration bundledConfig() throws Exception {
        try (InputStream in = Objects.requireNonNull(PluginConfigTest.class.getResourceAsStream("/config.yml"))) {
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }
}
