package gabrielbot.core.listeners;

import br.com.brjdevs.highhacks.eventbus.Listener;
import gabrielbot.GabrielBot;
import gabrielbot.core.data.GabrielData;
import gabrielbot.core.listeners.operations.ReactionOperations;
import gabrielbot.utils.stats.MessageStats;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ReadyEvent;
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
            GuildMessageReceivedEvent gmre = (GuildMessageReceivedEvent)event;
            User author = gmre.getAuthor();
            if(author.isBot() || author.isFake()) return;
            TextChannel tc = gmre.getChannel();
            if(!tc.canTalk()) return;
            if(!PermissionUtil.checkPermission(tc, tc.getGuild().getSelfMember(), Permission.MESSAGE_EMBED_LINKS)) {
                tc.sendMessage("I need the Embed Links permission").queue();
                return;
            }
            if(gmre.getMessage().getRawContent().equals("f")) {
                if(GabrielData.guilds().get().get(gmre.getGuild().getId()).payRespects) {
                    tc.sendMessage("You have paid your respects. \uD83C\uDF46").queue();
                }
                return;
            }

            try {
                GabrielBot.getInstance().registry.process(gmre);
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
        } else if(event instanceof ReadyEvent) {
            JDA jda = event.getJDA();
            JDA.ShardInfo info = jda.getShardInfo();
            GabrielBot.LOGGER.info(info == null ? "Bot ready!" : "Shard " + info.getShardId() + " ready!");
        }
    }
}
