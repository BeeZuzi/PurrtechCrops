package eu.purrtech.purrTechCrops;

import eu.purrtech.purrTechCrops.command.PtcCommand;
import eu.purrtech.purrTechCrops.config.PluginConfig;
import eu.purrtech.purrTechCrops.harvest.HarvestService;
import eu.purrtech.purrTechCrops.harvest.HarvestToggle;
import eu.purrtech.purrTechCrops.listener.CropInteractListener;
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
     * Re-reads config.yml from disk and swaps the active configuration.
     */
    public void reloadPluginConfig() {
        reloadConfig();
        pluginConfig = PluginConfig.load(getConfig(), getLogger());
        getLogger().info("Loaded " + pluginConfig.crops().size() + " crop(s).");
    }

    public PluginConfig pluginConfig() {
        return pluginConfig;
    }
}
