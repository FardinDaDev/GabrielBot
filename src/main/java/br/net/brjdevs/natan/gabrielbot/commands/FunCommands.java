package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.commands.fun.BrainfuckInterpreter;
import br.net.brjdevs.natan.gabrielbot.commands.fun.Jokes;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandCategory;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandRegistry;
import br.net.brjdevs.natan.gabrielbot.core.command.RegisterCommand;
import br.net.brjdevs.natan.gabrielbot.core.command.SimpleCommand;
import net.dv8tion.jda.core.entities.Message;

@RegisterCommand.Class
public class FunCommands {
    @RegisterCommand
    public static void brainfuck(CommandRegistry cr) {
        BrainfuckInterpreter interpreter = new BrainfuckInterpreter(20_000, 1<<12); //20k ops, 4K ram
        cr.register("brainfuck", SimpleCommand.builder(CommandCategory.FUN)
                .description("Evaluates brainfuck code")
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
                            out.isEmpty() ? "No returns" : out
                    ).queue();
                })
                .build()
        );
    }

    @RegisterCommand
    public static void joke(CommandRegistry cr) {
        cr.register("joke", SimpleCommand.builder(CommandCategory.FUN)
                .description("Sends a joke")
                .help((thiz, event)->thiz.helpEmbed(event, "joke",
                        "`>>joke`\n" +
                                "`>>joke @Someone`\n" +
                                "`>>joke Someone`"
                ))
                .code((thiz, event, args)->{
                    Message m = event.getMessage();
                    event.getChannel().sendMessage(Jokes.getJoke(args.length == 0 ? event.getAuthor().getAsMention() : String.join(" ", args))).queue();
                })
                .build()
        );
    }
}
