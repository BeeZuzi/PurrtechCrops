package eu.purrtech.purrTechCrops.api.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Called when a player right-click harvests a fully grown crop, before anything changes in the world.
 * <p>
 * Cancelling the event leaves the crop untouched. The drop list is mutable; the replant item
 * has already been removed from it when the crop is going to be replanted.
 */
public final class CropHarvestEvent extends BlockEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final List<ItemStack> drops;
    private boolean replant;
    private boolean cancelled;

    public CropHarvestEvent(@NotNull Player player, @NotNull Block block, @NotNull List<ItemStack> drops, boolean replant) {
        super(block);
        this.player = player;
        this.drops = drops;
        this.replant = replant;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    /**
     * Mutable list of items the player receives.
     */
    public @NotNull List<ItemStack> getDrops() {
        return drops;
    }

    /**
     * Whether the crop is replanted (reset to age 0). If false, the block is removed.
     */
    public boolean isReplant() {
        return replant;
    }

    /**
     * Setting this to false returns the replant item to the drops and removes the block.
     * Setting it to true replants the crop without consuming a replant item.
     */
    public void setReplant(boolean replant) {
        this.replant = replant;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
