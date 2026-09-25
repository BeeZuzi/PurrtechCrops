package eu.purrtech.purrTechCrops;

import eu.purrtech.purrTechCrops.config.PluginConfig;
import eu.purrtech.purrTechCrops.harvest.HarvestService;
import eu.purrtech.purrTechCrops.listener.CropInteractListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class PurrTechCrops extends JavaPlugin {

    private volatile PluginConfig pluginConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPluginConfig();

        HarvestService harvestService = new HarvestService(() -> pluginConfig);
        getServer().getPluginManager().registerEvents(new CropInteractListener(harvestService), this);
    }

    /**
     * Re-reads config.yml from disk and swaps the active configuration.
     */
    public void reloadPluginConfig() {
        reloadConfig();
        pluginConfig = PluginConfig.load(getConfig(), getLogger());
        getLogger().info("Loaded " + pluginConfig.crops().size() + " crop(s).");
    }
}
