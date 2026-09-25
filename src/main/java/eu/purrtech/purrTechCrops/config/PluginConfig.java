package eu.purrtech.purrTechCrops.config;

import eu.purrtech.purrTechCrops.crop.CropDefinition;
import eu.purrtech.purrTechCrops.crop.CropRegistry;
import eu.purrtech.purrTechCrops.harvest.DropMode;
import eu.purrtech.purrTechCrops.harvest.ReplantFallback;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Immutable snapshot of config.yml. A reload creates a new instance.
 * Invalid values are logged and replaced by defaults, so a broken config never disables the plugin.
 */
public record PluginConfig(
        CropRegistry crops,
        Set<String> disabledWorlds,
        boolean requireHoe,
        boolean disableWhenSneaking,
        boolean applyFortune,
        boolean damageHoe,
        DropMode dropMode,
        ReplantFallback replantFallback,
        boolean creativeDrops,
        boolean allowAdventure,
        boolean strictProtection
) {

    public static PluginConfig load(FileConfiguration config, Logger logger) {
        return new PluginConfig(
                loadCrops(config.getConfigurationSection("crops"), logger),
                Set.copyOf(config.getStringList("disabled-worlds")),
                config.getBoolean("harvest.require-hoe", false),
                config.getBoolean("harvest.disable-when-sneaking", false),
                config.getBoolean("harvest.apply-fortune", true),
                config.getBoolean("harvest.damage-hoe", true),
                parseEnum(config, "harvest.drop-mode", DropMode.GROUND, logger),
                parseEnum(config, "harvest.replant-fallback", ReplantFallback.FREE, logger),
                config.getBoolean("harvest.creative-drops", false),
                config.getBoolean("harvest.allow-adventure", false),
                config.getBoolean("protection.strict", false)
        );
    }

    private static CropRegistry loadCrops(ConfigurationSection section, Logger logger) {
        if (section == null) {
            logger.warning("Missing 'crops' section in config.yml, using default crops.");
            return CropRegistry.defaults();
        }
        List<CropDefinition> definitions = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            Material block = Material.matchMaterial(key);
            if (block == null || !block.isBlock() || !isSupportedCrop(block.createBlockData())) {
                logger.warning("crops." + key + ": '" + key + "' is not a supported crop block, skipping.");
                continue;
            }
            String itemName = section.getString(key, "");
            Material replantItem = Material.matchMaterial(itemName);
            if (replantItem == null || !replantItem.isItem()) {
                logger.warning("crops." + key + ": '" + itemName + "' is not a valid item, skipping.");
                continue;
            }
            definitions.add(new CropDefinition(block, replantItem));
        }
        return new CropRegistry(definitions);
    }

    // Crops must have an age; two-block-tall crops (pitcher plant) cannot be replanted by resetting the age.
    private static boolean isSupportedCrop(BlockData data) {
        return data instanceof Ageable && !(data instanceof Bisected);
    }

    private static <E extends Enum<E>> E parseEnum(FileConfiguration config, String path, E fallback, Logger logger) {
        String value = config.getString(path);
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(fallback.getDeclaringClass(), value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            logger.warning(path + ": '" + value + "' is not valid, using " + fallback.name() + ".");
            return fallback;
        }
    }
}
