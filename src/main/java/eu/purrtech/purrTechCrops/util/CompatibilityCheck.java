package eu.purrtech.purrTechCrops.util;

import org.bukkit.plugin.PluginManager;

import java.util.List;
import java.util.logging.Logger;

/**
 * Warns about installed plugins that react to block breaks, which strict protection mode fires for every harvest.
 */
public final class CompatibilityCheck {

    private static final List<String> BLOCK_BREAK_PLUGINS = List.of("mcMMO", "AuraSkills", "Jobs");

    private CompatibilityCheck() {
    }

    public static void run(PluginManager pluginManager, boolean strictProtection, Logger logger) {
        if (!strictProtection) {
            return;
        }
        List<String> found = BLOCK_BREAK_PLUGINS.stream()
                .filter(name -> pluginManager.getPlugin(name) != null)
                .toList();
        if (!found.isEmpty()) {
            logger.warning("protection.strict is enabled and " + String.join(", ", found)
                    + " is installed. Each harvest fires a BlockBreakEvent, so these plugins treat it as a broken"
                    + " crop (XP, payments, replant abilities). Disable protection.strict if this is not wanted.");
        }
    }
}
