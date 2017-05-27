package br.net.brjdevs.natan.gabrielbot.commands.custom;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.HashMap;
import java.util.Map;

public abstract class CustomCommand {
    public static final String PREFIX_EMBED = "embed:";
    public static final String PREFIX_CODE = "code:";

    public final String process(GuildMessageReceivedEvent event, String input, String... mappings) {
        if(mappings.length % 2 == 1) {
            throw new IllegalArgumentException("Mappings must have an even number of elements");
        }
        Map<String, String> map = new HashMap<>();
        for(int i = 0; i < mappings.length; i+=2) {
            map.put(mappings[i], mappings[i+1]);
        }
        return process(event, input, map);
    }

    public abstract String process(GuildMessageReceivedEvent event, String input, Map<String, String> mappings);
    public abstract String getRaw();

    public static CustomCommand of(GuildMessageReceivedEvent event, String text) {
        String lower = text.toLowerCase();
        if(lower.startsWith(PREFIX_EMBED)) {
            return new EmbedCustomCommand(text.substring(PREFIX_EMBED.length()));
        } else if(lower.startsWith(PREFIX_CODE)) {
            return CodeCustomCommand.of(event, text.substring(PREFIX_CODE.length()));
        } else {
            return new TextCustomCommand(text);
        }
    }
}
