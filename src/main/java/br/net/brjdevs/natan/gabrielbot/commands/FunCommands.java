package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.core.command.Argument;
import br.net.brjdevs.natan.gabrielbot.core.command.Command;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandCategory;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandPermission;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandReference;
import br.net.brjdevs.natan.gabrielbot.utils.Utils;
import br.net.brjdevs.natan.gabrielbot.utils.commands.Jokes;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;

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
        int sumFirst = first.getAsMention().chars().sum();
        int sumSecond = second.getAsMention().chars().sum();

        int diff = Math.abs((sumFirst % 101) - (sumSecond % 101));

        int love = 100-diff;

        String heart = love > 80 ? "\uD83D\uDC96" : love > 50 ? "\uD83D\uDC93" : love > 30 ? "\u2665" : "\uD83D\uDC94";

        channel.sendMessage(new EmbedBuilder()
                .setTitle("\u2763 Love Calculator \u2763")
                .appendDescription(heart + " " + first.getName() + "\n")
                .appendDescription(heart + " " + second.getName() + "\n")
                .appendDescription("\n")
                .appendDescription(love + "% " + Utils.progressBar((double)love/100, 40))
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
