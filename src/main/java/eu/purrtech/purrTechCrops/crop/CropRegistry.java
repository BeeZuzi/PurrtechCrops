package eu.purrtech.purrTechCrops.crop;

import org.bukkit.Material;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lookup of supported crops by their block material.
 */
public final class CropRegistry {

    private final Map<Material, CropDefinition> crops;

    public CropRegistry(Collection<CropDefinition> definitions) {
        Map<Material, CropDefinition> map = new EnumMap<>(Material.class);
        for (CropDefinition definition : definitions) {
            map.put(definition.block(), definition);
        }
        this.crops = Collections.unmodifiableMap(map);
    }

    /**
     * Vanilla crops supported out of the box; used when the config has no crops section.
     */
    public static CropRegistry defaults() {
        return new CropRegistry(List.of(
                new CropDefinition(Material.WHEAT, Material.WHEAT_SEEDS),
                new CropDefinition(Material.CARROTS, Material.CARROT),
                new CropDefinition(Material.POTATOES, Material.POTATO),
                new CropDefinition(Material.BEETROOTS, Material.BEETROOT_SEEDS),
                new CropDefinition(Material.NETHER_WART, Material.NETHER_WART),
                new CropDefinition(Material.COCOA, Material.COCOA_BEANS)
        ));
    }

    public Optional<CropDefinition> find(Material block) {
        return Optional.ofNullable(crops.get(block));
    }

    public int size() {
        return crops.size();
    }
}
