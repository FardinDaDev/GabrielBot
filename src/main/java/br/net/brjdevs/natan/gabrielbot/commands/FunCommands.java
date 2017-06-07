package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.commands.fun.BrainfuckInterpreter;
import br.net.brjdevs.natan.gabrielbot.commands.fun.Jokes;
import br.net.brjdevs.natan.gabrielbot.core.command.Argument;
import br.net.brjdevs.natan.gabrielbot.core.command.Command;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandCategory;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandPermission;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandReference;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class FunCommands {
    @Command(
            name = "brainfuck",
            description = "Evaluates brainfuck code",
            usage = "`>>brainfuck <code> <input>`" +
            "\n\n" +
            "`>>brainfuck ++++++++++[>+++++++>++++++++++>+++>+<<<<-]>++.>+.+++++++..+++.>++.<<+++++++++++++++.>.+++.------.--------.>+.>.`: Prints \"Hello World!\"",
            permission = CommandPermission.USER,
            category = CommandCategory.FUN
    )
    public static void brainfuck(@Argument("this") CommandReference thiz, @Argument("event") GuildMessageReceivedEvent event, @Argument("args") String[] args, @Argument("channel") TextChannel channel) {
        if (args.length == 0) {
            thiz.onHelp(event);
            return;
        }
        try {
            BrainfuckInterpreter interpreter = new BrainfuckInterpreter(20_000, 1 << 12); //20k ops, 4K ram
            String out = interpreter.process(args[0].toCharArray(), args.length == 1 ? "" : args[1]);
            channel.sendMessage(out.isEmpty() ? "No returns" : out).queue();
        } catch (BrainfuckInterpreter.BrainfuckException ex) {
            channel.sendMessage(ex.getMessage()).queue();
        }
    }

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
