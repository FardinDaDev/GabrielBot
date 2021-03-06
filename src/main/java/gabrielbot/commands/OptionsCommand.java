package gabrielbot.commands;

import br.com.brjdevs.java.utils.functions.TriConsumer;
import com.google.common.base.Preconditions;
import gabrielbot.core.command.Argument;
import gabrielbot.core.command.Command;
import gabrielbot.core.command.CommandCategory;
import gabrielbot.core.command.CommandPermission;
import gabrielbot.core.command.CommandReference;
import gabrielbot.core.data.GabrielData;
import gabrielbot.utils.Utils;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class OptionsCommand {
    private static final Map<String, TriConsumer<CommandReference, GuildMessageReceivedEvent, String[]>> options = new HashMap<>();

    static {
        registerOption("nsfw:toggle", (event, args) -> {
            String channel = event.getChannel().getId();
            GabrielData.ChannelData data = GabrielData.channels().get().get(channel);
            if (data == null) GabrielData.channels().get().set(channel, data = new GabrielData.ChannelData());
            data.nsfw = !data.nsfw;
            event.getChannel().sendMessage("NSFW has been " + (data.nsfw ? "enabled" : "disabled") + " for this channel").queue();
        });
        registerOption("prefix:set", (thiz, event, args) -> {
            if (args.length == 0) {
                thiz.onHelp(event);
                return;
            }
            String prefix = args[0];
            String guild = event.getGuild().getId();

            GabrielData.GuildCommandData data = GabrielData.guildCommands().get().get(guild);
            if (prefix.equals(GabrielData.config().prefix)) {
                data.prefix = prefix;
                event.getChannel().sendMessage("Removed custom prefix").queue();
                return;
            }
            if (data == null) GabrielData.guildCommands().get().set(guild, data = new GabrielData.GuildCommandData());
            data.prefix = prefix;
            event.getChannel().sendMessage("Prefix has been changed to " + GabrielData.guildCommands().get().get(guild).prefix).queue();
        });
        registerOption("starboard:enable", (thiz, event, args) -> {
            if (args.length == 0) {
                thiz.onHelp(event);
                return;
            }
            String channel = args[0].replaceAll("(<#)?(\\d+?)(>)?", "$2");
            long id;
            try {
                id = Long.parseLong(channel);
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("`" + args[0] + "` is not a valid number").queue();
                return;
            }
            TextChannel tc = event.getGuild().getTextChannelById(id);
            if (tc == null || !tc.canTalk()) {
                event.getChannel().sendMessage("Unable to add message to starboard, check that the configured channel exists and I can talk there").queue();
                return;
            }
            GabrielData.GuildData data = GabrielData.guilds().get().get(event.getGuild().getId());
            if (data == null) {
                GabrielData.guilds().get().set(event.getGuild().getId(), data = new GabrielData.GuildData());
            }
            data.starboardChannelId = id;
            event.getChannel().sendMessage("Starboard channel set to " + tc.getName()).queue();
        });
        registerOption("starboard:disable", (event, args) -> {
            GabrielData.GuildData data = GabrielData.guilds().get().get(event.getGuild().getId());
            if (data != null) data.starboardChannelId = 0;
            event.getChannel().sendMessage("Starboard disabled").queue();
        });
        registerOption("starboard:check", (event, args) -> {
            GabrielData.GuildData data = GabrielData.guilds().get().get(event.getGuild().getId());
            if (data != null && data.starboardChannelId != 0) {
                TextChannel tc = event.getGuild().getTextChannelById(data.starboardChannelId);
                if(tc == null || !tc.canTalk()) {
                    event.getChannel().sendMessage("Invalid channel configured for starboard: either the configured one was deleted, or I cannot speak in it, so I will be resetting the configuration").queue();
                    data.starboardChannelId = 0;
                    return;
                }
                if(data.starboardBlacklist == null) data.starboardBlacklist = new TLongHashSet();
                EmbedBuilder eb = new EmbedBuilder();
                eb.addField("Channel", "Starboard channel is set to " + tc.getAsMention(), true);
                eb.addField("Minimum stars", "" + data.minStars, true);
                eb.addBlankField(true);
                eb.addField("Maximum message age", data.maxStarboardMessageAgeMillis == 0 ? "No max age set" : (data.maxStarboardMessageAgeMillis/1000) + " second(s)", false);
                String blacklist;
                if(data.starboardBlacklist.isEmpty()) {
                    blacklist = "No one is blacklisted :D";
                } else {
                    blacklist = Arrays.stream(data.starboardBlacklist.toArray()).mapToObj(id->"<@" + id + ">").collect(Collectors.joining(", "));
                    if(blacklist.length() > 1024) {
                        blacklist = Utils.paste(Arrays.stream(data.starboardBlacklist.toArray()).mapToObj(id->event.getGuild().getMemberById(id)).filter(Objects::nonNull).map(m->String.format("%#s", m)).collect(Collectors.joining(", ")));
                    }
                }
                eb.addField("Blacklist", blacklist, false);
                event.getChannel().sendMessage(eb.build()).queue();
                return;
            }
            event.getChannel().sendMessage("No configured starboard channel").queue();
        });
        registerOption("starboard:min", (thiz, event, args) -> {
            if (args.length == 0) {
                thiz.onHelp(event);
                return;
            }
            int min;
            try {
                min = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("`" + args[0] + "` is not a valid number").queue();
                return;
            }
            if (min < 1) {
                event.getChannel().sendMessage("Minimum must be greater than 0").queue();
                return;
            }
            GabrielData.GuildData data = GabrielData.guilds().get().get(event.getGuild().getId());
            if (data == null) {
                GabrielData.guilds().get().set(event.getGuild().getId(), data = new GabrielData.GuildData());
            }
            data.minStars = min;
            event.getChannel().sendMessage("Minimum number of stars to add a message to the starboard is now " + min).queue();
        });
        registerOption("starboard:blacklist", (thiz, event, args) -> {
            if (args.length < 2 || !(args[0].equals("add") || args[0].equals("remove"))) {
                thiz.onHelp(event);
                return;
            }
            String mention = args[1].replaceAll("(<@!?)?(\\d+?)(>)?", "$2");
            long id;
            try {
                id = Long.parseLong(mention);
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("`" + args[1] + "` is not a valid mention/user id").queue();
                return;
            }
            GabrielData.GuildData data = GabrielData.guilds().get().get(event.getGuild().getId());
            if (data == null) {
                GabrielData.guilds().get().set(event.getGuild().getId(), data = new GabrielData.GuildData());
            }
            TLongSet blacklist = data.starboardBlacklist;
            if (blacklist == null) {
                blacklist = data.starboardBlacklist = new TLongHashSet();
            }
            if (args[0].equals("add")) {
                if (blacklist.add(id)) {
                    event.getChannel().sendMessage("Successfully blacklisted user").queue();
                } else {
                    event.getChannel().sendMessage("User already blacklisted").queue();
                }
            } else {
                if (blacklist.remove(id)) {
                    event.getChannel().sendMessage("Successfully removed user from the blacklist").queue();
                } else {
                    event.getChannel().sendMessage("User is not blacklisted").queue();
                }
            }
        });
        registerOption("starboard:maxage", (thiz, event, args) -> {
            if (args.length == 0) {
                thiz.onHelp(event);
                return;
            }
            long age;
            try {
                age = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("`" + args[0] + "` is not a valid number").queue();
                return;
            }
            if (age < 0) {
                event.getChannel().sendMessage("Age must be positive").queue();
                return;
            }
            GabrielData.GuildData data = GabrielData.guilds().get().get(event.getGuild().getId());
            if (age == 0) {
                if (data != null) data.maxStarboardMessageAgeMillis = 0;
                event.getChannel().sendMessage("Removed starboard max age").queue();
                return;
            }
            if (data == null) {
                GabrielData.guilds().get().set(event.getGuild().getId(), data = new GabrielData.GuildData());
            }
            data.maxStarboardMessageAgeMillis = age * 1000;
            event.getChannel().sendMessage("Messages older than " + age + " seconds won't be added to the starboard anymore").queue();
        });
        registerOption("payrespects:toggle", ((event, args) -> {
            GabrielData.GuildData data = GabrielData.guilds().get().get(event.getGuild().getId());
            if(data == null) GabrielData.guilds().get().set(event.getGuild().getId(), data = new GabrielData.GuildData());
            data.payRespects = !data.payRespects;
            event.getChannel().sendMessage(data.payRespects ? "Enabled paying respects" : "Disabled paying respects").queue();
        }));
    }

    public static void registerOption(String name, TriConsumer<CommandReference, GuildMessageReceivedEvent, String[]> code) {
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkArgument(!name.isEmpty(), "Name is empty");
        Preconditions.checkNotNull(code, "code");
        options.putIfAbsent(name, code);
    }

    public static void registerOption(String name, BiConsumer<GuildMessageReceivedEvent, String[]> code) {
        Preconditions.checkNotNull(code, "code");
        registerOption(name, (ignored, event, args) -> code.accept(event, args));
    }

    @Command(
            name = "opts",
            description = "Changes local options",
            usage = "`>>opts nsfw toggle`: toggles nsfw on the channel it's run\n" +
                    "`>>opts prefix set <prefix>`: changes the prefix for this guild\n" +
                    "`>>opts starboard enable <channel mention>`: Enables starboard on specified channel\n" +
                    "`>>opts starboard disable`: Disables starboard\n" +
                    "`>>opts starboard min <number>`: Sets minimum stars needed to add messages to the starboard\n" +
                    "`>>opts starboard blacklist add <@mention>`: Blacklists mentioned user from adding messages to the starboard\n" +
                    "`>>opts starboard blacklist remove <@mention>`: Removes the mentioned user from the starboard blacklist\n" +
                    "`>>opts starboard maxage <seconds>`: Messages older than the specified time won't be added to the starboard\n" +
                    "`>>opts starboard check`: Check starboard configuration\n" +
                    "`>>opts payrespects toggle`: Toggles the 'You have paid your respects' message",
            permission = CommandPermission.ADMIN,
            category = CommandCategory.MODERATION
    )
    public static void opts(@Argument("this") CommandReference thiz, @Argument("event")GuildMessageReceivedEvent event, @Argument("args") String[] args) {
        if (args.length < 2) {
            thiz.onHelp(event);
            return;
        }
        String name = "";
        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            if (!name.isEmpty()) name += ":";
            name += s;
            TriConsumer<CommandReference, GuildMessageReceivedEvent, String[]> option = options.get(name);
            if (option != null) {
                String[] a;
                if (++i < args.length) a = Arrays.copyOfRange(args, i, args.length);
                else a = new String[0];
                option.accept(thiz, event, a);
                return;
            }
        }
        thiz.onHelp(event);
    }
}
