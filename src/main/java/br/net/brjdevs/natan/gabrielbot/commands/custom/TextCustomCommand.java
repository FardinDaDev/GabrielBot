package br.net.brjdevs.natan.gabrielbot.commands.custom;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.Map;

public class TextCustomCommand extends CustomCommand {
    private final String text;

    public TextCustomCommand(String text) {
        this.text = text;
    }

    @Override
    public String process(GuildMessageReceivedEvent event, String input, Map<String, String> mappings) {
        return map(text, mappings);
    }

    @Override
    public String getRaw() {
        return text;
    }
}
