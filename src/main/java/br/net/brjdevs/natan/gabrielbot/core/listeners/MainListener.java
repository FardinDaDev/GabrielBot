package br.net.brjdevs.natan.gabrielbot.core.listeners;

import br.com.brjdevs.highhacks.eventbus.Listener;
import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.utils.stats.MessageStats;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.utils.PermissionUtil;

public class MainListener implements EventListener {
    @Override
    @Listener
    public void onEvent(Event event) {
        if(event instanceof GenericMessageEvent) {
            User author = ((GenericMessageEvent) event).getAuthor();
            if(author.isBot() || author.isFake()) return;
            if(event instanceof GuildMessageReceivedEvent) {
                TextChannel tc = ((GuildMessageReceivedEvent) event).getChannel();
                if(!tc.canTalk()) return;
                if(!PermissionUtil.checkPermission(tc, tc.getGuild().getSelfMember(), Permission.MESSAGE_EMBED_LINKS)) {
                    tc.sendMessage("I need the Embed Links permission").queue();
                    return;
                }
                GabrielBot.getInstance().registry.process((GuildMessageReceivedEvent)event);
                return;
            }
            return;
        }
        if(event instanceof MessageReceivedEvent) {
            if(((MessageReceivedEvent) event).isFromType(ChannelType.TEXT)) MessageStats.message();
            else ((MessageReceivedEvent) event).getChannel().sendMessage("Only works at guilds").queue();
        }
    }
}
