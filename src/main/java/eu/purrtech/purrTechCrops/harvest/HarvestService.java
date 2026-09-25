package eu.purrtech.purrTechCrops.harvest;

import eu.purrtech.purrTechCrops.config.PluginConfig;
import eu.purrtech.purrTechCrops.crop.CropDefinition;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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

    public HarvestService(Supplier<PluginConfig> config) {
        this.config = config;
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

    private static boolean canHarvest(Player player, Block block, PluginConfig config) {
        if (config.disabledWorlds().contains(block.getWorld().getName())) {
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
     */
    public void harvest(Player player, Block block, CropDefinition crop) {
        PluginConfig config = this.config.get();
        Ageable ageable = (Ageable) block.getBlockData();
        boolean creative = player.getGameMode() == GameMode.CREATIVE;
        ItemStack hand = player.getInventory().getItemInMainHand();

        // Drops must be computed before the block changes; the tool applies Fortune.
        ItemStack tool = config.applyFortune() ? hand : ItemStack.empty();
        List<ItemStack> drops = new ArrayList<>(block.getDrops(tool, player));

        boolean replant = consumeOne(drops, crop.replantItem())
                || creative
                || replantWithoutDrop(player, crop, config.replantFallback());
        if (replant) {
            Ageable replanted = (Ageable) ageable.clone();
            replanted.setAge(0);
            block.setBlockData(replanted, true);
        } else {
            block.setType(Material.AIR);
        }

        if (!creative || config.creativeDrops()) {
            giveDrops(player, block, drops, config.dropMode());
        }
        if (!creative && config.damageHoe() && isHoe(hand)) {
            player.damageItemStack(EquipmentSlot.HAND, 1);
        }
    }

    private static boolean replantWithoutDrop(Player player, CropDefinition crop, ReplantFallback fallback) {
        return switch (fallback) {
            case FREE -> true;
            case SKIP -> false;
            case INVENTORY -> {
                PlayerInventory inventory = player.getInventory();
                ItemStack one = ItemStack.of(crop.replantItem());
                if (!inventory.containsAtLeast(one, 1)) {
                    yield false;
                }
                inventory.removeItem(one);
                yield true;
            }
        };
    }

    private static void giveDrops(Player player, Block block, List<ItemStack> drops, DropMode mode) {
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
