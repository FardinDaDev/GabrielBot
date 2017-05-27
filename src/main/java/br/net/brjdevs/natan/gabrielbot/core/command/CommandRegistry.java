package br.net.brjdevs.natan.gabrielbot.core.command;

import br.net.brjdevs.natan.gabrielbot.commands.custom.CustomCommand;
import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import br.net.brjdevs.natan.gabrielbot.utils.Utils;
import br.net.brjdevs.natan.gabrielbot.utils.data.JedisDataManager;
import br.net.brjdevs.natan.gabrielbot.utils.stats.MessageStats;
import com.google.common.base.Preconditions;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class CommandRegistry implements BiConsumer<String, Command> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRegistry.class);

    private final Map<String, Command> commands;

    public CommandRegistry(Map<String, Command> commands) {
        this.commands = Preconditions.checkNotNull(commands);
    }

    public CommandRegistry() {
        this(new HashMap<>());
    }

    @Override
    public void accept(String s, Command command) {
        register(s, command);
    }

    public void register(String s, Command c) {
        commands.putIfAbsent(s, c);
    }

    public void process(GuildMessageReceivedEvent event) {
        String guildid = event.getGuild().getId(),
               authorid = event.getAuthor().getId();
        JedisDataManager blacklist = GabrielData.blacklist();
        if(blacklist.get(guildid) != null || blacklist.get(authorid) != null) return;
        String first = event.getMessage().getRawContent().split(" ")[0];
        String prefix = GabrielData.config().prefix;
        GabrielData.GuildCommandData data = GabrielData.guildCommands().get().get(event.getGuild().getId());
        String guildPrefix = data == null ? null : data.prefix;
        if(first.startsWith(prefix) || (guildPrefix != null && first.startsWith(guildPrefix))) {
            String cmdname = first.substring(first.startsWith(prefix) ? prefix.length() : guildPrefix.length());
            Command cmd = commands.get(cmdname);
            if(cmd == null) {
                handleCustom(event, cmdname);
                return;
            }
            if(!cmd.permission().test(event.getMember())) {
                if(cmd.permission() == CommandPermission.PREMIUM) {
                    event.getChannel().sendMessage("This feature is [premium](https://www.patreon.com/gabrielbot) only").queue();
                    return;
                }
                event.getChannel().sendMessage("You don't have permission to do this").queue();
                return;
            }
            MessageStats.command();
            try {
                cmd.run(event);
            } catch(Throwable t) {
                LOGGER.error("Error running command " + cmdname, t);
                event.getChannel().sendMessage("There was an unexpected error running the command, don't worry, it's already been reported and will be fixed as soon as possible").queue();
            }
        }
    }

    public void handleCustom(GuildMessageReceivedEvent event, String cmdname) {
        GabrielData.GuildCommandData data = GabrielData.guildCommands().get().get(event.getGuild().getId());
        if(data == null || data.custom.isEmpty() || !data.custom.contains(cmdname)) return;
        CustomCommand custom = GabrielData.guilds().get().get(event.getGuild().getId()).customCommands.get(cmdname);
        String rawInput = event.getMessage().getRawContent();

        User user = event.getAuthor();
        Member m = event.getMember();
        Guild g = event.getGuild();
        Message msg = event.getMessage();
        TextChannel c = event.getChannel();
        String processed = custom.process(event, rawInput.substring(rawInput.indexOf(cmdname)+cmdname.length()),
                "user", event.getAuthor().getName(),
                "discrim", event.getAuthor().getDiscriminator(),
                "mention", event.getAuthor().getAsMention(),
                "channel", event.getChannel().getName(),
                "userid", event.getAuthor().getId(),
                "channelid", event.getChannel().getId(),
                "guild", event.getGuild().getName(),
                "guildid", event.getGuild().getId(),
                "event.message.raw", msg.getRawContent(),
                "event.message.stripped", msg.getStrippedContent(),
                "event.message", msg.getContent(),
                "event.author.mention", user.getAsMention(),
                "event.author.username", user.getName(),
                "event.author.discriminator", user.getDiscriminator(),
                "event.author.name", m.getEffectiveName(),
                "event.guild.name", g.getName(),
                "event.guild.owner.name", g.getOwner().getEffectiveName(),
                "event.guild.owner.username", g.getOwner().getUser().getName(),
                "event.guild.owner.discriminator", g.getOwner().getUser().getDiscriminator(),
                "event.channel.name", c.getName(),
                "event.channel.topic", c.getTopic(),
                "event.channel.mention", c.getAsMention()
        );
        if(processed == null) return;
        if(processed.length() > 1990) {
            processed = "Done! " + Utils.paste(processed);
        }
        event.getChannel().sendMessage(processed).queue();
    }

    public Map<String, Command> commands() {
        return commands;
    }
}
