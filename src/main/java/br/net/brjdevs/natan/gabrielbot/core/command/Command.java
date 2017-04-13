package br.net.brjdevs.natan.gabrielbot.core.command;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@FunctionalInterface
public interface Command {
    void run(GuildMessageReceivedEvent event);
    default CommandPermission permission() {
        return CommandPermission.USER;
    }

    default String description() {
        throw new UnsupportedOperationException();
    }

    default Message help() {
        throw new UnsupportedOperationException();
    }
}
