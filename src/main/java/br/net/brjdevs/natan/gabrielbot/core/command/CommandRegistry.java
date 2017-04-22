package br.net.brjdevs.natan.gabrielbot.core.command;

import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import br.net.brjdevs.natan.gabrielbot.utils.data.JedisDataManager;
import br.net.brjdevs.natan.gabrielbot.utils.stats.MessageStats;
import com.google.common.base.Preconditions;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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
        GabrielData.GuildData data = GabrielData.guilds().get().get(event.getGuild().getId());
        String guildPrefix = data == null ? null : data.prefix;
        if(first.startsWith(prefix) || (guildPrefix != null && first.startsWith(guildPrefix))) {
            String cmdname = first.substring(first.startsWith(prefix) ? prefix.length() : guildPrefix.length());
            Command cmd = commands.get(cmdname);
            if(cmd == null) {
                handleCustom(event, cmdname);
                return;
            }
            if(!cmd.permission().test(event.getMember())) {
                event.getChannel().sendMessage("You don't have permission to do this").queue();
                return;
            }
            MessageStats.command();
            try {
                cmd.run(event);
            } catch(Throwable t) {
                LOGGER.error("Error running command " + cmdname, t);
                event.getChannel().sendMessage("There was an unexpected " + t.getClass().getSimpleName() + " running the command").queue();
            }
        }
    }

    public void handleCustom(GuildMessageReceivedEvent event, String cmdname) {
        GabrielData.GuildData gd = GabrielData.guilds().get().get(event.getGuild().getId());
        if(gd == null || gd.customCommands == null || gd.customCommands.isEmpty()) return;
        String custom = gd.customCommands.get(cmdname);
        event.getChannel().sendMessage(custom).queue();
    }

    public Map<String, Command> commands() {
        return commands;
    }
}
