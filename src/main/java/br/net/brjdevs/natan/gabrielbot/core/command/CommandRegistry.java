package br.net.brjdevs.natan.gabrielbot.core.command;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import br.net.brjdevs.natan.gabrielbot.utils.stats.MessageStats;
import com.google.common.base.Preconditions;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRegistry.class);

    private final Map<String, Command> commands;

    public CommandRegistry(Map<String, Command> commands) {
        this.commands = Preconditions.checkNotNull(commands);
    }

    public CommandRegistry() {
        this(new HashMap<>());
    }

    public void register(String s, Command c) {
        commands.putIfAbsent(s, c);
    }

    public void process(GuildMessageReceivedEvent event) {
        String first = event.getMessage().getRawContent().split(" ")[0];
        String p = GabrielData.config().prefix;
        if(first.startsWith(p)) {
            String cmdname = first.substring(p.length());
            Command cmd = commands.get(cmdname);
            if(cmd == null) return;
            if(!cmd.permission().test(event.getGuild(), event.getMember())) {
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

    public Map<String, Command> commands() {
        return Collections.unmodifiableMap(commands);
    }
}
