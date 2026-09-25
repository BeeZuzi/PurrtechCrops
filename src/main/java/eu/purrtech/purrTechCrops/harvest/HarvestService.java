package eu.purrtech.purrTechCrops.harvest;

import eu.purrtech.purrTechCrops.crop.CropDefinition;
import eu.purrtech.purrTechCrops.crop.CropRegistry;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Harvests fully grown crops and replants them in place.
 */
public final class HarvestService {

    private final CropRegistry registry;

    public HarvestService(CropRegistry registry) {
        this.registry = registry;
    }

    /**
     * Returns the crop definition if the block is a supported crop at its maximum age.
     */
    public Optional<CropDefinition> findHarvestable(Block block) {
        if (!(block.getBlockData() instanceof Ageable ageable) || ageable.getAge() < ageable.getMaximumAge()) {
            return Optional.empty();
        }
        return registry.find(block.getType());
    }

    /**
     * Drops the crop's loot (minus one replant item) and resets the crop to age 0.
     * The caller is responsible for checking {@link #findHarvestable(Block)} first.
     */
    public void harvest(Player player, Block block, CropDefinition crop) {
        Ageable ageable = (Ageable) block.getBlockData();

        // Drops must be computed before the block changes; the tool applies Fortune.
        ItemStack tool = player.getInventory().getItemInMainHand();
        List<ItemStack> drops = new ArrayList<>(block.getDrops(tool, player));
        // Vanilla loot tables always yield a replant item; if a datapack removes it, replant for free.
        consumeOne(drops, crop);

        Ageable replanted = (Ageable) ageable.clone();
        replanted.setAge(0);
        block.setBlockData(replanted, true);

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        Location dropLocation = block.getLocation().toCenterLocation();
        for (ItemStack drop : drops) {
            block.getWorld().dropItemNaturally(dropLocation, drop);
        }
    }

    private static void consumeOne(Collection<ItemStack> drops, CropDefinition crop) {
        Iterator<ItemStack> iterator = drops.iterator();
        while (iterator.hasNext()) {
            ItemStack drop = iterator.next();
            if (drop.getType() != crop.replantItem()) {
                continue;
            }
            if (drop.getAmount() > 1) {
                drop.setAmount(drop.getAmount() - 1);
            } else {
                iterator.remove();
            }
            return;
        }
    }
}
