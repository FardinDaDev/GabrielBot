package br.net.brjdevs.natan.gabrielbot.core.command;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public interface Command {
    void run(GuildMessageReceivedEvent event);
    CommandPermission permission();

    String description(GuildMessageReceivedEvent event);

    MessageEmbed help(GuildMessageReceivedEvent event);

    CommandCategory category();

    boolean isHiddenFromHelp();
}
