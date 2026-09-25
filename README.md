# PurrTechCrops

Right-click a fully grown crop to harvest it — the crop is replanted automatically and one seed is taken from the drops as the cost of replanting.

## Features

- Wheat, carrots, potatoes, beetroots, nether wart and cocoa out of the box (cocoa keeps its facing)
- Uses the vanilla loot tables, so Fortune and datapacks work as usual
- Respects land protection plugins, with an optional strict mode for protections that only guard block breaking
- Residence integration via the `harvest` flag
- Drops to the ground or straight into the inventory
- Optional hoe requirement and hoe durability
- Vanilla break sound, particles and hand swing
- Players can turn it off for themselves with `/ptc toggle`
- MiniMessage messages, English and Czech included
- `CropHarvestEvent` API for other plugins

## Requirements

- Paper 1.21.11 (or a fork)
- Java 21

## Installation

1. Put `PurrTechCrops-<version>.jar` into `plugins/`.
2. Start the server. `plugins/PurrTechCrops/config.yml` and `plugins/PurrTechCrops/lang/` are created.
3. Adjust the config and run `/ptc reload`.

## Commands and permissions

| Command | Permission | Default | Description |
|---------|------------|---------|-------------|
| — | `purrtechcrops.use` | everyone | Harvest crops with a right click |
| `/ptc toggle` | `purrtechcrops.toggle` | everyone | Turn right-click harvesting on/off for yourself (saved across restarts) |
| `/ptc reload` | `purrtechcrops.admin` | op | Reload the config and language file |

`/purrtechcrops` works as well.

## Configuration

Every option is documented in [config.yml](src/main/resources/config.yml). Highlights:

| Option | Default | Description |
|--------|---------|-------------|
| `crops` | 6 vanilla crops | `CROP_BLOCK: REPLANT_ITEM`. Any single-block crop with an age works. |
| `language` | `en` | Loads `lang/<language>.yml`. Copy `en.yml` to add a translation. |
| `harvest.drop-mode` | `GROUND` | `GROUND` or `INVENTORY` (leftovers drop on the ground) |
| `harvest.replant-fallback` | `FREE` | What to do if the loot has no replant item (only possible with custom loot tables): `FREE`, `INVENTORY`, `SKIP` |
| `harvest.require-hoe` | `false` | Only harvest with a hoe in hand |
| `protection.strict` | `false` | Also fire a `BlockBreakEvent` for every harvest, see below |
| `hooks.residence` | `true` | Check the Residence `harvest` flag, see below |

Invalid values are reported in the console and replaced by defaults — the plugin never fails to start because of the config. New options from plugin updates are added to your config automatically.

## Compatibility

**Land protection.** Harvesting is skipped whenever a protection plugin denies interacting with the crop (WorldGuard, GriefPrevention, Lands, … do this by default). If your protection only blocks *breaking* blocks, enable `protection.strict`.

**Residence.** Residence only protects right-click harvesting of sweet berries and cave vines by itself, so PurrTechCrops checks the Residence `harvest` flag for every crop (`/res set harvest true|false`, also per player with `/res pset`). Residence admins (`/resadmin`) bypass it. Denied players see a message in the action bar. Requires no setup; disable with `hooks.residence: false`.

**Strict mode side effects.** With `protection.strict` enabled, every harvest fires a `BlockBreakEvent`. Plugins such as mcMMO, AuraSkills or Jobs will then treat a harvest as a broken crop (XP, payments, replant abilities). The plugin warns about this in the console when it detects them.

**Other right-click harvest plugins.** A harvested crop is reset to age 0, so a second harvest plugin will not harvest it again — but you should still only run one.

## API

Listen to `eu.purrtech.purrTechCrops.api.event.CropHarvestEvent`. It is called before anything changes in the world:

```java
@EventHandler
public void onHarvest(CropHarvestEvent event) {
    if (event.getBlock().getType() == Material.WHEAT) {
        event.getDrops().add(new ItemStack(Material.BREAD)); // drops are mutable
    }
    // event.setReplant(false) -> seed goes back to the drops, block is removed
    // event.setCancelled(true) -> nothing happens
}
```

Add `PurrTechCrops` to `softdepend` in your `plugin.yml`.

## Building

```bash
./gradlew build
```

The jar is written to `build/libs/`. `./gradlew runServer` starts a Paper 1.21.11 test server with the plugin.
