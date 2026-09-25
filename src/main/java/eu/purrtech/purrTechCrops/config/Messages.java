package eu.purrtech.purrTechCrops.config;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;

/**
 * MiniMessage templates from the "messages" config section. Every message may use the {@code <prefix>} tag.
 * An empty message is not sent.
 */
public record Messages(
        String prefix,
        String reloaded,
        String toggledOn,
        String toggledOff,
        String onlyPlayers
) {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    static Messages load(ConfigurationSection config) {
        return new Messages(
                config.getString("messages.prefix", ""),
                config.getString("messages.reloaded", ""),
                config.getString("messages.toggled-on", ""),
                config.getString("messages.toggled-off", ""),
                config.getString("messages.only-players", "")
        );
    }

    public void send(Audience audience, String template, TagResolver... placeholders) {
        if (template.isEmpty()) {
            return;
        }
        TagResolver resolver = TagResolver.resolver(
                TagResolver.resolver(placeholders),
                Placeholder.parsed("prefix", prefix)
        );
        audience.sendMessage(MINI_MESSAGE.deserialize(template, resolver));
    }
}
