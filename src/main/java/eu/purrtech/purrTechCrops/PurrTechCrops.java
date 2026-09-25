package eu.purrtech.purrTechCrops;

import eu.purrtech.purrTechCrops.command.PtcCommand;
import eu.purrtech.purrTechCrops.config.ConfigUpdater;
import eu.purrtech.purrTechCrops.config.LanguageLoader;
import eu.purrtech.purrTechCrops.config.Messages;
import eu.purrtech.purrTechCrops.config.PluginConfig;
import eu.purrtech.purrTechCrops.harvest.HarvestService;
import eu.purrtech.purrTechCrops.harvest.HarvestToggle;
import eu.purrtech.purrTechCrops.listener.CropInteractListener;
import eu.purrtech.purrTechCrops.util.CompatibilityCheck;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class PurrTechCrops extends JavaPlugin {

    private volatile PluginConfig pluginConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPluginConfig();

        HarvestToggle toggle = new HarvestToggle(this);
        HarvestService harvestService = new HarvestService(this::pluginConfig, toggle);
        getServer().getPluginManager().registerEvents(new CropInteractListener(harvestService), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(PtcCommand.create(this, toggle),
                        "Manage PurrTechCrops", List.of(PtcCommand.ALIAS)));
    }

    /**
     * Re-reads config.yml and the language file from disk and swaps the active configuration.
     */
    public void reloadPluginConfig() {
        reloadConfig();
        List<String> addedKeys = ConfigUpdater.update(getConfig());
        if (!addedKeys.isEmpty()) {
            saveConfig();
            getLogger().info("Updated config.yml, added: " + String.join(", ", addedKeys));
        }

        Messages messages = new LanguageLoader(this).load(getConfig().getString("language", LanguageLoader.DEFAULT_LANGUAGE));
        pluginConfig = PluginConfig.load(getConfig(), messages, getLogger());
        getLogger().info("Loaded " + pluginConfig.crops().size() + " crop(s).");
        CompatibilityCheck.run(getServer().getPluginManager(), pluginConfig.strictProtection(), getLogger());
    }

    public PluginConfig pluginConfig() {
        return pluginConfig;
    }
}
