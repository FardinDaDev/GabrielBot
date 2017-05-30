package br.net.brjdevs.natan.gabrielbot.core.listeners;

import br.com.brjdevs.highhacks.eventbus.Listener;
import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import br.net.brjdevs.natan.gabrielbot.core.listeners.operations.ReactionOperations;
import br.net.brjdevs.natan.gabrielbot.utils.stats.MessageStats;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.utils.PermissionUtil;

import java.util.concurrent.Future;

public class MainListener implements EventListener {
    @Override
    @Listener
    public void onEvent(Event event) {
        if(event instanceof GuildMessageReceivedEvent) {
            User author = ((GuildMessageReceivedEvent) event).getAuthor();
            if(author.isBot() || author.isFake()) return;
            TextChannel tc = ((GuildMessageReceivedEvent) event).getChannel();
            if(!tc.canTalk()) return;
            if(!PermissionUtil.checkPermission(tc, tc.getGuild().getSelfMember(), Permission.MESSAGE_EMBED_LINKS)) {
                tc.sendMessage("I need the Embed Links permission").queue();
                return;
            }
            try {
                GabrielBot.getInstance().registry.process((GuildMessageReceivedEvent)event);
            } catch(Throwable t) {
                GabrielBot.LOGGER.error("Error on MainListener", t);
            }
        } else if(event instanceof MessageReceivedEvent) {
            MessageReceivedEvent mre = (MessageReceivedEvent)event;
            if(mre.isFromType(ChannelType.TEXT)) {
                MessageStats.message();
            }
            else {
                if(mre.getAuthor().isBot()) return;
                mre.getChannel().sendMessage("Only works at guilds").queue();
            }
        } else if(event instanceof GuildJoinEvent) {
            Guild g = ((GuildJoinEvent)event).getGuild();
            User u = g.getOwner().getUser();
            GabrielBot.getInstance().log("Joined guild " + String.format("%s (%s), owned by %s#%s", g.getName(), g.getId(), u.getName(), u.getDiscriminator()));
            GabrielBot.getInstance().getShard(g.getIdLong()).postStats();
        } else if(event instanceof GuildLeaveEvent) {
            Guild g = ((GuildLeaveEvent)event).getGuild();
            GabrielBot.getInstance().log("Left guild " + String.format("%s (%s)", g.getName(), g.getId()));
            GabrielBot.getInstance().removePlayer(g.getIdLong());
            g.getTextChannels().forEach(tc-> GabrielData.channels().get().remove(tc.getId()));
            GabrielData.guilds().get().remove(g.getId());
            GabrielData.guildCommands().get().remove(g.getId());
            GabrielBot.getInstance().getShard(g.getIdLong()).postStats();
        } else if(event instanceof MessageDeleteEvent) {
            Future<Void> future = ReactionOperations.get(((MessageDeleteEvent)event).getMessageIdLong());
            if(future != null) future.cancel(true);
        }
    }
}
