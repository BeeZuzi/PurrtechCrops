package eu.purrtech.purrTechCrops.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import eu.purrtech.purrTechCrops.PurrTechCrops;
import eu.purrtech.purrTechCrops.config.Messages;
import eu.purrtech.purrTechCrops.config.PluginConfig;
import eu.purrtech.purrTechCrops.harvest.HarvestToggle;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /purrtechcrops} (alias {@code /ptc}) with the {@code reload} and {@code toggle} subcommands.
 */
public final class PtcCommand {

    public static final String NAME = "purrtechcrops";
    public static final String ALIAS = "ptc";

    private static final String ADMIN_PERMISSION = "purrtechcrops.admin";
    private static final String TOGGLE_PERMISSION = "purrtechcrops.toggle";

    private PtcCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> create(PurrTechCrops plugin, HarvestToggle toggle) {
        return Commands.literal(NAME)
                .then(Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission(ADMIN_PERMISSION))
                        .executes(context -> {
                            plugin.reloadPluginConfig();
                            PluginConfig config = plugin.pluginConfig();
                            config.messages().send(context.getSource().getSender(), config.messages().reloaded(),
                                    Placeholder.unparsed("crops", String.valueOf(config.crops().size())));
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("toggle")
                        .requires(source -> source.getSender().hasPermission(TOGGLE_PERMISSION))
                        .executes(context -> {
                            Messages messages = plugin.pluginConfig().messages();
                            CommandSender sender = context.getSource().getSender();
                            // The executor differs from the sender when run via /execute as <player>.
                            if (!(context.getSource().getExecutor() instanceof Player player)) {
                                messages.send(sender, messages.onlyPlayers());
                                return 0;
                            }
                            boolean enabled = toggle.toggle(player);
                            messages.send(player, enabled ? messages.toggledOn() : messages.toggledOff());
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }
}
