package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.commands.fun.BrainfuckInterpreter;
import br.net.brjdevs.natan.gabrielbot.commands.fun.Jokes;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandCategory;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandRegistry;
import br.net.brjdevs.natan.gabrielbot.core.command.RegisterCommand;
import br.net.brjdevs.natan.gabrielbot.core.command.SimpleCommand;
import net.dv8tion.jda.core.entities.Message;

import static br.net.brjdevs.natan.gabrielbot.core.localization.LocalizationManager.*;

@RegisterCommand.Class
public class FunCommands {
    @RegisterCommand
    public static void brainfuck(CommandRegistry cr) {
        BrainfuckInterpreter interpreter = new BrainfuckInterpreter(20_000, 1<<12); //20k ops, 4K ram
        cr.register("brainfuck", SimpleCommand.builder(CommandCategory.FUN)
                .description("brainfuck", "Evaluates brainfuck code")
                .help((thiz, event)->thiz.helpEmbed(event, "brainfuck",
                        "`>>brainfuck <code> <input>`\n" +
                                "\n\n" +
                                "`>>brainfuck ++++++++++[>+++++++>++++++++++>+++>+<<<<-]>++.>+.+++++++..+++.>++.<<+++++++++++++++.>.+++.------.--------.>+.>.`: Prints \"Hello World!\""
                ))
                .code((thiz, event, args)->{
                    if(args.length == 0) {
                        thiz.onHelp(event);
                        return;
                    }
                    String out;
                    try {
                        out = interpreter.process(args[0].toCharArray(), args.length == 1 ? "" : args[1], event.getGuild());
                    } catch(BrainfuckInterpreter.BrainfuckException ex) {
                        event.getChannel().sendMessage(ex.getMessage()).queue();
                        return;
                    }
                    event.getChannel().sendMessage(
                            out.isEmpty() ? getString(event.getGuild(), Fun.BRAINFUCK_NO_RETURNS, "No returns") : out
                    ).queue();
                })
                .build()
        );
    }

    @RegisterCommand
    public static void joke(CommandRegistry cr) {
        cr.register("joke", SimpleCommand.builder(CommandCategory.FUN)
                .description("joke", "Sends a joke")
                .help((thiz, event)->thiz.helpEmbed(event, "joke",
                        "`>>joke`\n" +
                                "`>>joke @Someone`\n" +
                                "`>>joke Someone`"
                ))
                .code((thiz, event, args)->{
                    Message m = event.getMessage();
                    String user = m.getMentionedUsers().size() > 0 ? m.getMentionedUsers().get(0).getAsMention() : null;
                    if(user == null) {
                        if(args.length == 0) {
                            user = event.getAuthor().getAsMention();
                        }
                        else {
                            user = String.join(" ", args);
                        }
                    }
                    event.getChannel().sendMessage(Jokes.getJoke(user)).queue();
                })
                .build()
        );
    }
}
