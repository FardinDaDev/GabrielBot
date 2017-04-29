package br.net.brjdevs.natan.gabrielbot.commands;

import br.com.brjdevs.java.utils.functions.TriConsumer;
import br.net.brjdevs.natan.gabrielbot.core.command.*;
import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import com.google.common.base.Preconditions;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@RegisterCommand.Class
public class OptionsCommand {
    private static final Map<String, TriConsumer<SimpleCommand, GuildMessageReceivedEvent, String[]>> options = new HashMap<>();

    static {
        registerOption("nsfw:toggle", (event, args)->{
            String channel = event.getChannel().getId();
            GabrielData.ChannelData data = GabrielData.channels().get().get(channel);
            if(data == null) GabrielData.channels().get().set(channel, data = new GabrielData.ChannelData());
            data.nsfw = !data.nsfw;
            event.getChannel().sendMessage("NSFW has been " + (data.nsfw ? "enabled" : "disabled") + " for this channel").queue();
        });
        registerOption("prefix:set", (thiz, event, args)->{
            if(args.length == 0) {
                event.getChannel().sendMessage(thiz.help(event)).queue();
                return;
            }
            String prefix = args[0];
            String guild = event.getGuild().getId();

            GabrielData.GuildCommandData data = GabrielData.guildCommands().get().get(guild);
            if(prefix.equals(GabrielData.config().prefix)) {
                data.prefix = prefix;
                event.getChannel().sendMessage("Removed custom prefix").queue();
                return;
            }
            if(data == null) GabrielData.guildCommands().get().set(guild, data = new GabrielData.GuildCommandData());
            data.prefix = prefix;
            event.getChannel().sendMessage("Prefix has been changed to " + GabrielData.guildCommands().get().get(guild).prefix).queue();
        });
    }

    public static void registerOption(String name, TriConsumer<SimpleCommand, GuildMessageReceivedEvent, String[]> code) {
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkArgument(!name.isEmpty(), "Name is empty");
        Preconditions.checkNotNull(code, "code");
        options.putIfAbsent(name, code);
    }

    public static void registerOption(String name, BiConsumer<GuildMessageReceivedEvent, String[]> code) {
        Preconditions.checkNotNull(code, "code");
        registerOption(name, (ignored, event, args)->code.accept(event, args));
    }

    @RegisterCommand
    public static void register(CommandRegistry registry) {
        registry.register("opts", SimpleCommand.builder(CommandCategory.MODERATION)
                .permission(CommandPermission.ADMIN)
                .description("opts", "Changes local options")
                .help((thiz, event)->thiz.helpEmbed(event, "opts",
                        "`>>opts nsfw toggle`: toggles nsfw on the channel it's run\n" +
                               "`>>opts prefix set <prefix>`: changes the prefix for this guild"
                ))
                .code((thiz, event, args)->{
                    if(args.length < 2) {
                        thiz.onHelp(event);
                        return;
                    }
                    String name = "";
                    for(int i = 0; i < args.length; i++) {
                        String s = args[i];
                        if(!name.isEmpty()) name += ":";
                        name += s;
                        TriConsumer<SimpleCommand, GuildMessageReceivedEvent, String[]> option = options.get(name);
                        if(option != null) {
                            String[] a;
                            if(++i < args.length) a = Arrays.copyOfRange(args, i, args.length);
                            else a = new String[0];
                            option.accept(thiz, event, a);
                            return;
                        }
                    }
                    event.getChannel().sendMessage(thiz.help(event)).queue();
                })
                .build());
    }
}
