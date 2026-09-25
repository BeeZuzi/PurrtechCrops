package eu.purrtech.purrTechCrops.config;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

/**
 * MiniMessage templates from a language file. Every message may use the {@code <prefix>} tag.
 * An empty message is not sent.
 */
public record Messages(
        String prefix,
        String reloaded,
        String toggledOn,
        String toggledOff,
        String onlyPlayers,
        String harvestDenied
) {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public static Messages load(ConfigurationSection language) {
        return new Messages(
                string(language, "prefix"),
                string(language, "reloaded"),
                string(language, "toggled-on"),
                string(language, "toggled-off"),
                string(language, "only-players"),
                string(language, "harvest-denied")
        );
    }

    // getString(path, default) would ignore the bundled defaults, so null is mapped to "" here.
    private static String string(ConfigurationSection language, String path) {
        return Objects.requireNonNullElse(language.getString(path), "");
    }

    public void send(Audience audience, String template, TagResolver... placeholders) {
        if (!template.isEmpty()) {
            audience.sendMessage(render(template, placeholders));
        }
    }

    public void sendActionBar(Audience audience, String template, TagResolver... placeholders) {
        if (!template.isEmpty()) {
            audience.sendActionBar(render(template, placeholders));
        }
    }

    private Component render(String template, TagResolver... placeholders) {
        TagResolver resolver = TagResolver.resolver(
                TagResolver.resolver(placeholders),
                Placeholder.parsed("prefix", prefix)
        );
        return MINI_MESSAGE.deserialize(template, resolver);
    }
}
