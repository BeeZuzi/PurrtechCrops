package eu.purrtech.purrTechCrops.util;

import eu.purrtech.purrTechCrops.config.PluginConfig;
import org.bukkit.Effect;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

/**
 * Visual and sound feedback for a harvest.
 */
public final class Effects {

    private Effects() {
    }

    /**
     * @param harvested the crop's block data before it was replanted
     */
    public static void playHarvest(Player player, Block block, BlockData harvested, PluginConfig config) {
        if (config.swingHand()) {
            player.swingMainHand();
        }
        if (config.breakEffect()) {
            // Vanilla block break sound and particles, shown to all nearby players.
            block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, harvested);
        }
    }
}
