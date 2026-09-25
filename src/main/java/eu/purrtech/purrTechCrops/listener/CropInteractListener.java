package eu.purrtech.purrTechCrops.listener;

import eu.purrtech.purrTechCrops.harvest.HarvestService;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Filters right clicks on blocks and delegates crop harvesting to {@link HarvestService}.
 */
public final class CropInteractListener implements Listener {

    static final String USE_PERMISSION = "purrtechcrops.use";

    private final HarvestService harvestService;

    public CropInteractListener(HarvestService harvestService) {
        this.harvestService = harvestService;
    }

    // HIGH so protection plugins (usually LOW/NORMAL) decide first. Cancellation is checked via
    // useInteractedBlock(), because isCancelled() does not reflect a denied block interaction reliably.
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        // The event fires once per hand; handling only the main hand prevents double harvests.
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission(USE_PERMISSION)) {
            return;
        }

        harvestService.findHarvestable(player, block).ifPresent(crop -> {
            harvestService.harvest(player, block, crop);
            // Prevent the held item from being used (placing a block, bone meal, ...).
            event.setCancelled(true);
        });
    }
}
