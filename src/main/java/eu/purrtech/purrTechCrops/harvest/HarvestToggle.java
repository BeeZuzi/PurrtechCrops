package eu.purrtech.purrTechCrops.harvest;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Per-player on/off switch for right-click harvesting, stored in the player's persistent data.
 * Only the disabled state is stored, so players without data use the default (enabled).
 */
public final class HarvestToggle {

    private final NamespacedKey disabledKey;

    public HarvestToggle(Plugin plugin) {
        this.disabledKey = new NamespacedKey(plugin, "harvest-disabled");
    }

    public boolean isEnabled(Player player) {
        return !player.getPersistentDataContainer().has(disabledKey);
    }

    /**
     * Flips the player's setting.
     *
     * @return true if harvesting is now enabled
     */
    public boolean toggle(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        if (data.has(disabledKey)) {
            data.remove(disabledKey);
            return true;
        }
        data.set(disabledKey, PersistentDataType.BOOLEAN, true);
        return false;
    }
}
