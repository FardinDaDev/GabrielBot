package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandPermission;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandRegistry;
import br.net.brjdevs.natan.gabrielbot.core.command.RegisterCommand;
import br.net.brjdevs.natan.gabrielbot.core.command.SimpleCommand;
import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import com.google.common.base.Preconditions;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@RegisterCommand.Class
public class OptionsCommand {
    private static final Map<String, BiConsumer<GuildMessageReceivedEvent, String[]>> options = new HashMap<>();

    static {
        registerOption("nsfw:toggle", (event, args)->{
            String channel = event.getChannel().getId();
            GabrielData.ChannelData data = GabrielData.channels().get().get(channel);
            if(data == null) GabrielData.channels().get().set(channel, data = new GabrielData.ChannelData());
            data.nsfw = !data.nsfw;
            event.getChannel().sendMessage("NSFW has been toggled for this channel").queue();
        });
    }

    public static void registerOption(String name, BiConsumer<GuildMessageReceivedEvent, String[]> code) {
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkArgument(!name.isEmpty(), "Name is empty");
        Preconditions.checkNotNull(code, "code");
        options.putIfAbsent(name, code);
    }

    @RegisterCommand
    public static void register(CommandRegistry registry) {
        registry.register("opts", SimpleCommand.builder()
                .permission(CommandPermission.ADMIN)
                .description("Changes local options")
                .help(SimpleCommand.helpEmbed("opts", CommandPermission.ADMIN,
                        "Changes local options for this guild",
                        "`>>opts nsfw toggle`: toggles nsfw on the channel it's run"
                ))
                .code((event, args)->{
                    if(args.length < 2) {
                        event.getChannel().sendMessage(GabrielBot.getInstance().registry.commands().get("opts").help()).queue();
                        return;
                    }
                    String name = "";
                    for(int i = 0; i < args.length; i++) {
                        String s = args[i];
                        if(!name.isEmpty()) name += ":";
                        name += s;
                        BiConsumer<GuildMessageReceivedEvent, String[]> option = options.get(name);
                        if(option != null) {
                            String[] a;
                            if(i+1 < args.length) a = Arrays.copyOfRange(args, i+1, args.length);
                            else a = new String[0];
                            option.accept(event, a);
                            return;
                        }
                    }
                    event.getChannel().sendMessage(GabrielBot.getInstance().registry.commands().get("opts").help()).queue();
                })
                .build());
    }
}
