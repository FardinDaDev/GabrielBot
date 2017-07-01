package gabrielbot.commands;

import gabrielbot.core.command.Argument;
import gabrielbot.core.command.Command;
import gabrielbot.core.command.CommandCategory;
import gabrielbot.core.command.CommandPermission;
import gabrielbot.core.command.CommandReference;
import gabrielbot.utils.Utils;
import gabrielbot.utils.commands.EmoteReference;
import gabrielbot.utils.commands.Jokes;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("unused")
public class FunCommands {
    @Command(
            name = "joke",
            description = "Sends a joke",
            usage = "`>>joke`\n" +
                    "`>>joke @Someone`\n" +
                    "`>>joke Chuck Norris`",
            permission = CommandPermission.USER,
            category = CommandCategory.FUN
    )
    public static void joke(@Argument("channel") TextChannel channel, @Argument("args") String[] args, @Argument("author") User author) {
        channel.sendMessage(Jokes.getJoke(args.length == 0 ? author.getAsMention() : String.join(" ", args))).queue();
    }

    @Command(
            name = "roll",
            nameArgs = {"(\\d{1,5})?"},
            description = "Rolls a dice",
            usage = "`>>roll`: Rolls a 6-sided dice once\n" +
                    "`>>roll <times>`: Rolls a 6-sided dice\n" +
                    "`>>roll<X>`: Rolls a X-sided dice once\n" +
                    "`>>roll<X> <times>`: Roll a X sided dice\n\n" +
                    "Number of sides must be between 1 and 99999\n" +
                    "**Notice there aren't spaces between `>>roll` and the number of sides**\n" +
                    "For example: `>>roll6 1`",
            permission = CommandPermission.USER,
            category = CommandCategory.MISC
    )
    public static void roll(@Argument("channel") TextChannel channel, @Argument("namearg-1") String size, @Argument("args") String[] args) {
        int sides = size == null ? 6 : Integer.parseInt(size); //Argument is already validated
        int times;
        if(args.length == 0) {
            times = 1;
        } else {
            try {
                times = Math.min(100, Math.max(Integer.parseInt(args[0]), 1));
            } catch(NumberFormatException e) {
                channel.sendMessage(EmoteReference.ERROR + "Not a valid number: " + args[0]).queue();
                return;
            }
        }
        long total = ThreadLocalRandom.current().longs().limit(times).map(l->Math.abs(l%sides)).sum();
        channel.sendMessage(times == 1 ? "You rolled a " + total : "You rolled a total of " + total + " with " + times + " rolls").queue();
    }

    @Command(
            name = "love",
            description = "Calculates how much two people love each other <3",
            usage = "`>>love @Mention`: Calculates love between you and the mentioned user\n" +
                    "`>>love @FirstPerson @SecondPerson`: Calculates love between the two mentioned persons",
            permission = CommandPermission.USER,
            category = CommandCategory.FUN
    )
    public static void love(@Argument("channel") TextChannel channel, @Argument("member") Member member, @Argument("message") Message message) {
        List<User> mentioned = message.getMentionedUsers();
        if(mentioned.size() == 0) {
            channel.sendMessage("You have to mention someone!").queue();
            return;
        }
        User first, second;
        if(mentioned.size() == 1) {
            first = member.getUser();
            second = mentioned.get(0);
        } else {
            first = mentioned.get(0);
            second = mentioned.get(1);
        }
        int sumFirst = first.getAsMention().chars().map(i->i*i).sum();
        int sumSecond = second.getAsMention().chars().map(i->i*i).sum();

        int diff = Math.abs((sumFirst % 101) - (sumSecond % 101));

        int love = 100-diff;

        if(first.getIdLong() == 132584525296435200L || first.getIdLong() == 155867458203287552L) {
            love = second.getIdLong() == 155867458203287552L || second.getIdLong() == 132584525296435200L ? 9001 : love; //lars + kode
        } else if(first.getIdLong() == 232542027550556160L || first.getIdLong() == 302159895027908608L) {
            love = second.getIdLong() == 302159895027908608L || second.getIdLong() == 232542027550556160L ? 9001 : love; //bomb + shiina
        } else if(first.getIdLong() == 267207628965281792L || first.getIdLong() == 251260900252712962L) {
            love = second.getIdLong() == 251260900252712962L || second.getIdLong() == 267207628965281792L ? 9001 : love; //desii + ion
        } else if(first.getIdLong() == 182245310024777728L || first.getIdLong() == 301504284745531395L) {
            love = second.getIdLong() == 301504284745531395L || second.getIdLong() == 182245310024777728L ? 9001 : love; //me + gab <3
        }

        String heart = love > 80 ? "\uD83D\uDC96" : love > 50 ? "\uD83D\uDC93" : love > 30 ? "\u2665" : "\uD83D\uDC94";

        channel.sendMessage(new EmbedBuilder()
                .setTitle("\u2763 Love Calculator \u2763")
                .appendDescription(heart + " " + first.getName() + "\n")
                .appendDescription(heart + " " + second.getName() + "\n")
                .appendDescription("\n")
                .appendDescription((love > 100 ? "∞ \uD83D\uDC96 `" : love + " `") + Utils.progressBar((double)love/100, 40) + "`")
                .setColor(member.getColor())
                .build()).queue();
    }

    @Command(
            name = "reg",
            description = "Converts text to regional indicators",
            usage =  "`>>reg <text>`",
            permission = CommandPermission.USER,
            category = CommandCategory.FUN
    )
    public static void reg(@Argument("this") CommandReference thiz, @Argument("event") GuildMessageReceivedEvent event, @Argument("channel")TextChannel channel, @Argument("args") String[] args) {
        if(args.length == 0) {
            thiz.onHelp(event);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for(char c : String.join(" ", args).toLowerCase().toCharArray()) {
            if(c >= 'a' && c <= 'z') {
                sb.append(":regional_indicator_").append(c).append(":  ");
                continue;
            }
            switch (c) {
                case '0':
                    sb.append(":zero:  ");
                    break;
                case '1':
                    sb.append(":one:  ");
                    break;
                case '2':
                    sb.append(":two:  ");
                    break;
                case '3':
                    sb.append(":three:  ");
                    break;
                case '4':
                    sb.append(":four:  ");
                    break;
                case '5':
                    sb.append(":five:  ");
                    break;
                case '6':
                    sb.append(":six:  ");
                    break;
                case '7':
                    sb.append(":seven:  ");
                    break;
                case '8':
                    sb.append(":eight:  ");
                    break;
                case '9':
                    sb.append(":nine:  ");
                    break;
                case '!':
                    sb.append(":exclamation:  ");
                    break;
                case '?':
                    sb.append(":question:  ");
                    break;
                case '+':
                    sb.append(":heavy_plus_sign:  ");
                    break;
                case '-':
                    sb.append(":heavy_minus_sign:  ");
                    break;
                case '$':
                    sb.append(":heavy_dollar_sign:  ");
                    break;
                default:
                    sb.append(":interrobang:  ");
                    break;
            }
        }
        String s = sb.toString();
        if (s.length() > 1990) {
            channel.sendMessage("This message is too big to send. Sorry :(").queue();
        } else {
            channel.sendMessage(s).queue();
        }
    }
}
