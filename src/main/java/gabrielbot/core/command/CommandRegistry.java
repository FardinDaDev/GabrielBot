package gabrielbot.core.command;

import gabrielbot.commands.custom.CustomCommand;
import gabrielbot.core.data.GabrielData;
import gabrielbot.utils.Regex;
import gabrielbot.utils.Utils;
import gabrielbot.utils.data.JedisDataManager;
import gabrielbot.utils.stats.MessageStats;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public class CommandRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRegistry.class);

    private final Map<String, CommandReference> commands = new HashMap<>();
    private final ClassLoader classLoader;

    public CommandRegistry(ClassLoader classLoader) {
        if(classLoader == null) throw new NullPointerException("classLoader");
        this.classLoader = classLoader;
    }

    public void register(Class<?> commandClass) {
        for(Method m : commandClass.getMethods()) {
            Command command = m.getAnnotation(Command.class);
            if(command == null) continue;
            if(!Modifier.isStatic(m.getModifiers())) {
                throw new IllegalArgumentException("Method " + m + " has @Command but isn't static");
            }
            String name = command.name().trim();
            if(name.isEmpty()) throw new IllegalArgumentException("Empty command name for method " + m);
            commands.put(name, new CommandReference(command, m, classLoader));
        }
    }

    private CommandReference findCommand(String name, Map<String, Object> args) {
        CommandReference ref = commands.get(name);
        if(ref == null) {
            looking: for(Map.Entry<String, CommandReference> entry : commands.entrySet()) {
                CommandReference r = entry.getValue();
                Command cmd = r.command;
                if(!name.startsWith(cmd.name())) continue;
                if(cmd.name().length() > name.length()) continue;
                List<String> list = new ArrayList<>();
                String[] nameArgs = cmd.nameArgs();
                String s = name.substring(cmd.name().length());
                for(int i = 0; i < nameArgs.length; i++) {
                    Matcher m = Regex.pattern("^" + nameArgs[i]).matcher(s);
                    if(!m.find()) {
                        continue looking;
                    }
                    String arg = m.group();
                    s = s.substring(arg.length());
                    list.add(arg);
                }
                if(s.length() > 0) continue;
                ref = r;
                int i = 1;
                for(String arg : list) {
                    args.put("namearg-" + (i++), arg);
                }
                args.put("nameargs", Collections.unmodifiableList(list));
            }
        }
        return ref;
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
        String usedPrefix;
        if(first.startsWith(usedPrefix = prefix) || (guildPrefix != null && first.startsWith(usedPrefix = guildPrefix))) {
            String cmdname = first.substring(usedPrefix.length());
            Map<String, Object> map = new HashMap<>();
            CommandReference ref = findCommand(cmdname, map);
            if(ref == null) {
                handleCustom(event, cmdname);
                return;
            }
            Command cmd = ref.command;
            if(!cmd.permission().test(event.getMember())) {
                if(cmd.permission() == CommandPermission.PREMIUM) {
                    event.getChannel().sendMessage("This feature is [premium](https://www.patreon.com/gabrielbot) only").queue();
                    return;
                }
                event.getChannel().sendMessage("You don't have permission to do this").queue();
                return;
            }
            MessageStats.command();
            map.putAll(Utils.map(
                    "event", event,
                    "message", event.getMessage(),
                    "member", event.getMember(),
                    "author", event.getAuthor(),
                    "guild", event.getGuild(),
                    "channel", event.getChannel(),
                    "selfmember", event.getGuild().getSelfMember(),
                    "selfuser", event.getJDA().getSelfUser(),
                    "jda", event.getJDA(),
                    "guildid", guildid,
                    "prefix", guildPrefix,
                    "this", ref,
                    "prefix", usedPrefix
            ));
            try {
                ref.invoker.run(event, ref.command.advancedSplit(), map);
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
        if(custom == null) {
            data.custom.remove(cmdname);
            return;
        }
        String rawInput = event.getMessage().getRawContent();

        User user = event.getAuthor();
        Member m = event.getMember();
        Guild g = event.getGuild();
        Message msg = event.getMessage();
        TextChannel c = event.getChannel();
        String input = rawInput.substring(rawInput.indexOf(cmdname)+cmdname.length()).trim();
        String processed = custom.process(event, input,
                "user", event.getAuthor().getName(),
                "discrim", event.getAuthor().getDiscriminator(),
                "mention", event.getAuthor().getAsMention(),
                "channel", event.getChannel().getName(),
                "userid", event.getAuthor().getId(),
                "channelid", event.getChannel().getId(),
                "guild", event.getGuild().getName(),
                "guildid", event.getGuild().getId(),
                "event.message.raw", input,
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

    public Map<String, CommandReference> commands() {
        return commands;
    }
}

/**
 *
 * package commandsystem;

 import java.lang.reflect.Method;
 import java.lang.reflect.Modifier;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;

 public class CommandRegistry {
 private final Map<String, CommandReference> map = new HashMap<>();
 private final ClassLoader loader;

 public CommandRegistry(ClassLoader loader) {
 this.loader = loader == null ? ClassLoader.getSystemClassLoader() : loader;
 }

 public void register(Class<?> cls) {
 for(Method m : cls.getMethods()) {
 if(!Modifier.isStatic(m.getModifiers())) continue;
 Command command = m.getAnnotation(Command.class);
 if(command == null) continue;
 map.put(command.name() + command.nameArgs().length, new CommandReference(command, m, loader));
 }
 }

 public void process(String cmdName, Map<String, Object> args) {
 CommandReference ref = map.get(cmdName);
 if(ref == null) {
 looking: for(Map.Entry<String, CommandReference> entry : map.entrySet()) {
 CommandReference r = entry.getValue();
 Command cmd = r.command;
 if(!cmdName.startsWith(cmd.name())) continue;
 if(cmd.name().length() > cmdName.length()) continue;
 List<String> list = new ArrayList<>();
 String[] nameArgs = cmd.nameArgs();
 String s = cmdName.substring(cmd.name().length());
 for(int i = 0; i < nameArgs.length; i++) {
 Matcher m = Pattern.compile("^" + nameArgs[i], Pattern.CASE_INSENSITIVE|Pattern.MULTILINE).matcher(s);
 if(!m.find()) {
 continue looking;
 }
 String arg = m.group();
 s = s.substring(arg.length());
 list.add(arg);
 }
 if(s.length() > 0) continue;
 ref = r;
 int i = 1;
 for(String arg : list) {
 args.put("namearg-" + (i++), arg);
 }
 args.put("nameargs", Collections.unmodifiableList(list));
 }
 if(ref == null) return;
 }
 ref.invoker.invoke(args);
 }
 }

 */
