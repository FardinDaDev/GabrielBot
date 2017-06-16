package br.net.brjdevs.natan.gabrielbot.core.listeners.operations;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@FunctionalInterface
public interface InteractiveOperation extends Operation {
    int run(GuildMessageReceivedEvent event);
}
