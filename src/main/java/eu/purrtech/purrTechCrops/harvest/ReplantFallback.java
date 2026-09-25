package eu.purrtech.purrTechCrops.harvest;

/**
 * What to do when the drops contain no replant item.
 */
public enum ReplantFallback {
    /** Replant anyway. */
    FREE,
    /** Take one replant item from the player's inventory; leave the block empty if they have none. */
    INVENTORY,
    /** Do not replant; leave the block empty. */
    SKIP
}
