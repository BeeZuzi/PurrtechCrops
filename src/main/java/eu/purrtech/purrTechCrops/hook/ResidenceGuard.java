package eu.purrtech.purrTechCrops.hook;

import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.ResAdmin;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import eu.purrtech.purrTechCrops.config.PluginConfig;
import eu.purrtech.purrTechCrops.harvest.HarvestGuard;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

/**
 * Checks the Residence {@code harvest} flag. Residence itself only guards right-click harvesting of
 * sweet berries and cave vines, so without this hook crops in foreign residences could be harvested.
 * <p>
 * Only reference this class when Residence is enabled; it links against Residence classes.
 */
public final class ResidenceGuard implements HarvestGuard {

    private final Supplier<PluginConfig> config;

    private ResidenceGuard(Supplier<PluginConfig> config) {
        this.config = config;
    }

    // Returns the interface type so callers never load this class unless Residence is present.
    public static HarvestGuard create(Supplier<PluginConfig> config) {
        return new ResidenceGuard(config);
    }

    @Override
    public boolean canHarvest(Player player, Block block) {
        if (!config.get().residenceHook() || !Flags.harvest.isGlobalyEnabled() || ResAdmin.isResAdmin(player)) {
            return true;
        }
        // Same check and default Residence uses for its own harvest protection.
        return FlagPermissions.has(block.getLocation(), player, Flags.harvest, true);
    }
}
