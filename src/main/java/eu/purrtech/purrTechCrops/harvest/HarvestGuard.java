package eu.purrtech.purrTechCrops.harvest;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Integration point for protection plugins that do not block the right click themselves.
 */
@FunctionalInterface
public interface HarvestGuard {

    /**
     * @return false if the player must not harvest the crop at this block
     */
    boolean canHarvest(Player player, Block block);
}
