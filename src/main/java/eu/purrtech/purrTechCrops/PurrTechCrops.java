package eu.purrtech.purrTechCrops;

import eu.purrtech.purrTechCrops.crop.CropRegistry;
import eu.purrtech.purrTechCrops.harvest.HarvestService;
import eu.purrtech.purrTechCrops.listener.CropInteractListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class PurrTechCrops extends JavaPlugin {

    @Override
    public void onEnable() {
        HarvestService harvestService = new HarvestService(CropRegistry.defaults());
        getServer().getPluginManager().registerEvents(new CropInteractListener(harvestService), this);
    }
}
