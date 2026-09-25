package eu.purrtech.purrTechCrops.harvest;

import eu.purrtech.purrTechCrops.api.event.CropHarvestEvent;
import eu.purrtech.purrTechCrops.config.Messages;
import eu.purrtech.purrTechCrops.config.PluginConfig;
import eu.purrtech.purrTechCrops.listener.CropInteractListener;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Cocoa;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CropHarvestTest {

    private ServerMock server;
    private Plugin plugin;
    private WorldMock world;
    private PlayerMock player;
    private HarvestToggle toggle;
    private YamlConfiguration yaml;
    private PluginConfig config;

    /** Loot returned by the fake loot table; the mock server has none. */
    private List<ItemStack> loot;
    private ItemStack lastTool;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        world = server.addSimpleWorld("world");
        player = server.addPlayer();
        player.setGameMode(GameMode.SURVIVAL);
        player.addAttachment(plugin, "purrtechcrops.use", true);

        try (InputStream in = Objects.requireNonNull(getClass().getResourceAsStream("/config.yml"))) {
            yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        yaml.set("harvest.damage-hoe", false); // damageItemStack is not implemented by MockBukkit
        reloadConfig();

        loot = List.of(new ItemStack(Material.WHEAT), new ItemStack(Material.WHEAT_SEEDS, 3));
        toggle = new HarvestToggle(plugin);
        HarvestService service = new HarvestService(() -> config, toggle, (block, tool, p) -> {
            lastTool = tool;
            return loot.stream().map(ItemStack::clone).toList();
        });
        server.getPluginManager().registerEvents(new CropInteractListener(service), plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // --- core behaviour ---

    @Test
    void matureCropIsReplantedAndDropsLootMinusOneSeed() {
        Block block = crop(Material.WHEAT, true);

        PlayerInteractEvent event = rightClick(block);

        assertEquals(Material.WHEAT, block.getType());
        assertEquals(0, age(block));
        assertEquals(1, count(groundDrops(), Material.WHEAT));
        assertEquals(2, count(groundDrops(), Material.WHEAT_SEEDS));
        assertEquals(Event.Result.DENY, event.useItemInHand());
    }

    @Test
    void unripeCropIsIgnored() {
        Block block = crop(Material.WHEAT, false);
        setAge(block, 3);

        PlayerInteractEvent event = rightClick(block);

        assertEquals(3, age(block));
        assertTrue(groundDrops().isEmpty());
        assertEquals(Event.Result.DEFAULT, event.useItemInHand());
    }

    @Test
    void offHandClickIsIgnored() {
        Block block = crop(Material.WHEAT, true);

        rightClick(block, EquipmentSlot.OFF_HAND, event -> {
        });

        assertMatureAndUntouched(block);
    }

    @Test
    void deniedInteractionIsIgnored() {
        Block block = crop(Material.WHEAT, true);

        rightClick(block, EquipmentSlot.HAND, event -> event.setUseInteractedBlock(Event.Result.DENY));

        assertMatureAndUntouched(block);
    }

    @Test
    void playerWithoutPermissionIsIgnored() {
        Block block = crop(Material.WHEAT, true);
        player = server.addPlayer();
        player.addAttachment(plugin, "purrtechcrops.use", false);

        rightClick(block);

        assertMatureAndUntouched(block);
    }

    @Test
    void cropMissingFromConfigIsIgnored() {
        set("crops.WHEAT", null);
        Block block = crop(Material.WHEAT, true);

        rightClick(block);

        assertMatureAndUntouched(block);
    }

    @Test
    void cocoaKeepsItsFacing() {
        loot = List.of(new ItemStack(Material.COCOA_BEANS, 3));
        Block block = crop(Material.COCOA, true);
        Cocoa data = (Cocoa) block.getBlockData();
        data.setFacing(BlockFace.EAST);
        block.setBlockData(data);

        rightClick(block);

        Cocoa replanted = (Cocoa) block.getBlockData();
        assertEquals(0, replanted.getAge());
        assertEquals(BlockFace.EAST, replanted.getFacing());
        assertEquals(2, count(groundDrops(), Material.COCOA_BEANS));
    }

    // --- replant fallback when the loot has no replant item ---

    @Test
    void freeFallbackReplantsWithoutSeed() {
        loot = List.of(new ItemStack(Material.WHEAT));
        Block block = crop(Material.WHEAT, true);

        rightClick(block);

        assertEquals(0, age(block));
        assertEquals(1, count(groundDrops(), Material.WHEAT));
    }

    @Test
    void skipFallbackLeavesBlockEmpty() {
        set("harvest.replant-fallback", "SKIP");
        loot = List.of(new ItemStack(Material.WHEAT));
        Block block = crop(Material.WHEAT, true);

        rightClick(block);

        assertEquals(Material.AIR, block.getType());
    }

    @Test
    void inventoryFallbackTakesSeedFromInventory() {
        set("harvest.replant-fallback", "INVENTORY");
        loot = List.of(new ItemStack(Material.WHEAT));
        player.getInventory().addItem(new ItemStack(Material.WHEAT_SEEDS, 2));
        Block block = crop(Material.WHEAT, true);

        rightClick(block);

        assertEquals(0, age(block));
        assertEquals(1, count(Arrays.asList(player.getInventory().getContents()), Material.WHEAT_SEEDS));
    }

    @Test
    void inventoryFallbackWithoutSeedLeavesBlockEmpty() {
        set("harvest.replant-fallback", "INVENTORY");
        loot = List.of(new ItemStack(Material.WHEAT));
        Block block = crop(Material.WHEAT, true);

        rightClick(block);

        assertEquals(Material.AIR, block.getType());
        assertEquals(1, count(groundDrops(), Material.WHEAT));
    }

    // --- CropHarvestEvent API ---

    @Test
    void cancelledHarvestEventChangesNothing() {
        set("harvest.replant-fallback", "INVENTORY");
        loot = List.of(new ItemStack(Material.WHEAT));
        player.getInventory().addItem(new ItemStack(Material.WHEAT_SEEDS));
        listen(CropHarvestEvent.class, event -> event.setCancelled(true));
        Block block = crop(Material.WHEAT, true);

        PlayerInteractEvent click = rightClick(block);

        assertMatureAndUntouched(block);
        assertEquals(1, count(Arrays.asList(player.getInventory().getContents()), Material.WHEAT_SEEDS));
        assertEquals(Event.Result.DEFAULT, click.useItemInHand());
    }

    @Test
    void disablingReplantReturnsSeedAndRemovesCrop() {
        listen(CropHarvestEvent.class, event -> event.setReplant(false));
        Block block = crop(Material.WHEAT, true);

        rightClick(block);

        assertEquals(Material.AIR, block.getType());
        assertEquals(3, count(groundDrops(), Material.WHEAT_SEEDS));
    }

    @Test
    void harvestEventDropsCanBeModified() {
        listen(CropHarvestEvent.class, event -> event.getDrops().add(new ItemStack(Material.DIAMOND)));
        Block block = crop(Material.WHEAT, true);

        rightClick(block);

        assertEquals(1, count(groundDrops(), Material.DIAMOND));
    }

    // --- configuration options ---

    @Test
    void inventoryDropModeGivesDropsToPlayer() {
        set("harvest.drop-mode", "INVENTORY");
        Block block = crop(Material.WHEAT, true);

        rightClick(block);

        List<ItemStack> inventory = Arrays.asList(player.getInventory().getContents());
        assertEquals(1, count(inventory, Material.WHEAT));
        assertEquals(2, count(inventory, Material.WHEAT_SEEDS));
        assertTrue(groundDrops().isEmpty());
    }

    @Test
    void creativeReplantsWithoutDrops() {
        player.setGameMode(GameMode.CREATIVE);
        Block block = crop(Material.WHEAT, true);

        rightClick(block);

        assertEquals(0, age(block));
        assertTrue(groundDrops().isEmpty());
    }

    @Test
    void creativeDropsOptionGivesDropsInCreative() {
        set("harvest.creative-drops", true);
        player.setGameMode(GameMode.CREATIVE);
        Block block = crop(Material.WHEAT, true);

        rightClick(block);

        assertEquals(1, count(groundDrops(), Material.WHEAT));
    }

    @Test
    void adventureModeRequiresOption() {
        player.setGameMode(GameMode.ADVENTURE);
        Block block = crop(Material.WHEAT, true);

        rightClick(block);
        assertMatureAndUntouched(block);

        set("harvest.allow-adventure", true);
        rightClick(block);
        assertEquals(0, age(block));
    }

    @Test
    void toggledOffPlayerIsIgnored() {
        toggle.toggle(player);
        Block block = crop(Material.WHEAT, true);

        rightClick(block);
        assertMatureAndUntouched(block);

        toggle.toggle(player);
        rightClick(block);
        assertEquals(0, age(block));
    }

    @Test
    void disabledWorldIsIgnored() {
        set("disabled-worlds", List.of("world"));
        Block block = crop(Material.WHEAT, true);

        rightClick(block);

        assertMatureAndUntouched(block);
    }

    @Test
    void sneakingIsIgnoredWhenConfigured() {
        set("harvest.disable-when-sneaking", true);
        player.setSneaking(true);
        Block block = crop(Material.WHEAT, true);

        rightClick(block);

        assertMatureAndUntouched(block);
    }

    @Test
    void requireHoeNeedsHoeInHand() {
        set("harvest.require-hoe", true);
        Block block = crop(Material.WHEAT, true);

        rightClick(block);
        assertMatureAndUntouched(block);

        player.getInventory().setItemInMainHand(new ItemStack(Material.IRON_HOE));
        rightClick(block);
        assertEquals(0, age(block));
    }

    @Test
    void fortuneUsesHeldItemOnlyWhenEnabled() {
        ItemStack hoe = new ItemStack(Material.DIAMOND_HOE);
        player.getInventory().setItemInMainHand(hoe);

        rightClick(crop(Material.WHEAT, true));
        assertEquals(Material.DIAMOND_HOE, lastTool.getType());

        set("harvest.apply-fortune", false);
        rightClick(crop(Material.WHEAT, true));
        assertTrue(lastTool.isEmpty());
    }

    // --- strict protection ---

    @Test
    void strictModeRespectsCancelledBlockBreak() {
        set("protection.strict", true);
        listen(BlockBreakEvent.class, event -> event.setCancelled(true));
        Block block = crop(Material.WHEAT, true);

        PlayerInteractEvent click = rightClick(block);

        assertMatureAndUntouched(block);
        assertEquals(Event.Result.DEFAULT, click.useItemInHand());
    }

    @Test
    void strictModeHarvestsWhenBlockBreakIsAllowed() {
        set("protection.strict", true);
        Block block = crop(Material.WHEAT, true);

        rightClick(block);

        assertEquals(0, age(block));
        assertEquals(1, count(groundDrops(), Material.WHEAT));
    }

    @Test
    void strictModeRespectsDisabledDrops() {
        set("protection.strict", true);
        listen(BlockBreakEvent.class, event -> event.setDropItems(false));
        Block block = crop(Material.WHEAT, true);

        rightClick(block);

        assertEquals(0, age(block));
        assertTrue(groundDrops().isEmpty());
    }

    // --- helpers ---

    private void reloadConfig() {
        config = PluginConfig.load(yaml, Messages.load(new YamlConfiguration()), plugin.getLogger());
    }

    private void set(String path, Object value) {
        yaml.set(path, value);
        reloadConfig();
    }

    private Block crop(Material type, boolean mature) {
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(type);
        Ageable data = (Ageable) block.getBlockData();
        data.setAge(mature ? data.getMaximumAge() : 0);
        block.setBlockData(data);
        return block;
    }

    private static void setAge(Block block, int age) {
        Ageable data = (Ageable) block.getBlockData();
        data.setAge(age);
        block.setBlockData(data);
    }

    private static int age(Block block) {
        return ((Ageable) block.getBlockData()).getAge();
    }

    private void assertMatureAndUntouched(Block block) {
        Ageable data = (Ageable) block.getBlockData();
        assertEquals(data.getMaximumAge(), data.getAge());
        assertTrue(groundDrops().isEmpty());
    }

    private PlayerInteractEvent rightClick(Block block) {
        return rightClick(block, EquipmentSlot.HAND, event -> {
        });
    }

    private PlayerInteractEvent rightClick(Block block, EquipmentSlot hand, Consumer<PlayerInteractEvent> before) {
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK,
                player.getInventory().getItemInMainHand(), block, BlockFace.UP, hand);
        before.accept(event);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private <T extends Event> void listen(Class<T> type, Consumer<T> action) {
        server.getPluginManager().registerEvent(type, new Listener() {
        }, EventPriority.NORMAL, (listener, event) -> {
            if (type.isInstance(event)) {
                action.accept(type.cast(event));
            }
        }, plugin);
    }

    private List<ItemStack> groundDrops() {
        List<ItemStack> drops = new ArrayList<>();
        for (Item item : world.getEntitiesByClass(Item.class)) {
            drops.add(item.getItemStack());
        }
        return drops;
    }

    private static int count(Collection<ItemStack> items, Material type) {
        return items.stream()
                .filter(item -> item != null && item.getType() == type)
                .mapToInt(ItemStack::getAmount)
                .sum();
    }
}
