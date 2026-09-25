package eu.purrtech.purrTechCrops.crop;

import org.bukkit.Material;

/**
 * A crop that can be harvested with a right click.
 *
 * @param block       the crop block (e.g. {@link Material#WHEAT})
 * @param replantItem the item consumed from the drops to replant the crop (e.g. {@link Material#WHEAT_SEEDS})
 */
public record CropDefinition(Material block, Material replantItem) {
}
