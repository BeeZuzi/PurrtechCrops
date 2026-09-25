package eu.purrtech.purrTechCrops.harvest;

import eu.purrtech.purrTechCrops.api.event.CropHarvestEvent;
import eu.purrtech.purrTechCrops.config.PluginConfig;
import eu.purrtech.purrTechCrops.crop.CropDefinition;
import eu.purrtech.purrTechCrops.util.Effects;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Harvests fully grown crops and replants them in place.
 */
public final class HarvestService {

    private final Supplier<PluginConfig> config;
    private final HarvestToggle toggle;

    public HarvestService(Supplier<PluginConfig> config, HarvestToggle toggle) {
        this.config = config;
        this.toggle = toggle;
    }

    /**
     * Returns the crop definition if the player may harvest the block right now:
     * the block is a supported crop at its maximum age and the player passes all configured restrictions.
     */
    public Optional<CropDefinition> findHarvestable(Player player, Block block) {
        PluginConfig config = this.config.get();
        if (!(block.getBlockData() instanceof Ageable ageable) || ageable.getAge() < ageable.getMaximumAge()) {
            return Optional.empty();
        }
        Optional<CropDefinition> crop = config.crops().find(block.getType());
        if (crop.isEmpty() || !canHarvest(player, block, config)) {
            return Optional.empty();
        }
        return crop;
    }

    private boolean canHarvest(Player player, Block block, PluginConfig config) {
        if (!toggle.isEnabled(player) || config.disabledWorlds().contains(block.getWorld().getName())) {
            return false;
        }
        GameMode gameMode = player.getGameMode();
        if (gameMode == GameMode.SPECTATOR || (gameMode == GameMode.ADVENTURE && !config.allowAdventure())) {
            return false;
        }
        if (config.disableWhenSneaking() && player.isSneaking()) {
            return false;
        }
        return !config.requireHoe() || isHoe(player.getInventory().getItemInMainHand());
    }

    /**
     * Gives the crop's drops (minus one replant item) and resets the crop to age 0.
     * The caller is responsible for checking {@link #findHarvestable(Player, Block)} first.
     *
     * @return false if another plugin prevented the harvest
     */
    public boolean harvest(Player player, Block block, CropDefinition crop) {
        PluginConfig config = this.config.get();
        boolean creative = player.getGameMode() == GameMode.CREATIVE;
        boolean dropItems = !creative || config.creativeDrops();

        if (config.strictProtection()) {
            // Lets any protection plugin that only guards block breaking veto the harvest.
            BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
            if (!breakEvent.callEvent()) {
                return false;
            }
            dropItems &= breakEvent.isDropItems();
        }

        // Drops must be computed before the block changes; the tool applies Fortune.
        ItemStack hand = player.getInventory().getItemInMainHand();
        List<ItemStack> drops = new ArrayList<>();
        if (dropItems) {
            drops.addAll(block.getDrops(config.applyFortune() ? hand : ItemStack.empty(), player));
        }

        // Nothing is taken from the player's inventory until the harvest event has passed.
        boolean seedFromDrops = consumeOne(drops, crop.replantItem());
        boolean replant = seedFromDrops || creative || switch (config.replantFallback()) {
            case FREE -> true;
            case SKIP -> false;
            case INVENTORY -> player.getInventory().containsAtLeast(ItemStack.of(crop.replantItem()), 1);
        };
        boolean seedFromInventory = replant && !seedFromDrops && !creative
                && config.replantFallback() == ReplantFallback.INVENTORY;

        CropHarvestEvent harvestEvent = new CropHarvestEvent(player, block, drops, replant);
        if (!harvestEvent.callEvent()) {
            return false;
        }

        BlockData harvested = block.getBlockData();
        // A listener may have changed the block, so the crop is re-checked before replanting.
        if (harvestEvent.isReplant() && block.getBlockData() instanceof Ageable replanted) {
            if (seedFromInventory) {
                player.getInventory().removeItem(ItemStack.of(crop.replantItem()));
            }
            // getBlockData() returns a copy, so other properties (cocoa facing) are kept.
            replanted.setAge(0);
            block.setBlockData(replanted, true);
        } else {
            if (seedFromDrops) {
                drops.add(ItemStack.of(crop.replantItem()));
            }
            block.setType(Material.AIR);
        }

        Effects.playHarvest(player, block, harvested, config);
        giveDrops(player, block, harvestEvent.getDrops(), config.dropMode());
        if (!creative && config.damageHoe() && isHoe(hand)) {
            player.damageItemStack(EquipmentSlot.HAND, 1);
        }
        return true;
    }

    private static void giveDrops(Player player, Block block, List<ItemStack> drops, DropMode mode) {
        if (drops.isEmpty()) {
            return;
        }
        Collection<ItemStack> toGround = mode == DropMode.INVENTORY
                ? player.getInventory().addItem(drops.toArray(ItemStack[]::new)).values()
                : drops;
        Location dropLocation = block.getLocation().toCenterLocation();
        for (ItemStack drop : toGround) {
            block.getWorld().dropItemNaturally(dropLocation, drop);
        }
    }

    /**
     * Removes one item of the given type from the drops.
     *
     * @return false if the drops contain no such item
     */
    private static boolean consumeOne(Collection<ItemStack> drops, Material item) {
        Iterator<ItemStack> iterator = drops.iterator();
        while (iterator.hasNext()) {
            ItemStack drop = iterator.next();
            if (drop.getType() != item) {
                continue;
            }
            if (drop.getAmount() > 1) {
                drop.setAmount(drop.getAmount() - 1);
            } else {
                iterator.remove();
            }
            return true;
        }
        return false;
    }

    private static boolean isHoe(ItemStack item) {
        return Tag.ITEMS_HOES.isTagged(item.getType());
    }
}
