package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.core.command.Argument;
import br.net.brjdevs.natan.gabrielbot.core.command.Command;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandCategory;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandPermission;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandReference;
import br.net.brjdevs.natan.gabrielbot.utils.Utils;
import br.net.brjdevs.natan.gabrielbot.utils.brainfuck.BrainfuckInterpreter;
import br.net.brjdevs.natan.gabrielbot.utils.lua.InstructionLimitException;
import br.net.brjdevs.natan.gabrielbot.utils.lua.LuaHelper;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CodeCommands {
    @Command(
            name = "brainfuck",
            description = "Evaluates brainfuck code",
            usage = "`>>brainfuck <code> <input>`" +
                    "\n\n" +
                    "`>>brainfuck ++++++++++[>+++++++>++++++++++>+++>+<<<<-]>++.>+.+++++++..+++.>++.<<+++++++++++++++.>.+++.------.--------.>+.>.`: Prints \"Hello World!\"",
            permission = CommandPermission.USER,
            category = CommandCategory.CODE
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
            name = "lua",
            description = "Evaluates lua code",
            usage = "`>>lua <code>`",
            permission = CommandPermission.USER,
            category = CommandCategory.CODE,
            advancedSplit = false
    )
    public static void lua(@Argument("this") CommandReference thiz, @Argument("event")GuildMessageReceivedEvent event, @Argument("args") String[] args, @Argument("channel") TextChannel channel) {
        if(args.length == 0) {
            thiz.onHelp(event);
            return;
        }
        String code = String.join(" ", args).trim();
        StringBuilder sb = new StringBuilder();
        Globals globals = LuaHelper.setup(event, 10000, 1, "", sb);
        Thread thread = new Thread(() -> {
            try {
                LuaFunction tostring = globals.get("tostring").checkfunction();
                LuaValue v = globals.load(code).call();
                String s = tostring.call(v).tojstring();
                String t = sb.toString().trim();
                channel.sendMessage(v.isnil() ? t.isEmpty() ? "Executed successfully with no returns" : t.length() > 500 ? Utils.paste(t) : t : "Executed successfully and returned " + (s.length() > 500 ? Utils.paste(s) : String.format("```%n%s```", s))).queue();
            } catch(Throwable e) {
                if(e.getCause() instanceof InstructionLimitException) {
                    channel.sendMessage("Cycle limit exceeded").queue();
                    return;
                }
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.close();
                String s = sw.toString();
                if (s.length() > 500) {
                    s = Utils.paste(s);
                } else {
                    s = "```\n" + s + "```";
                }
                channel.sendMessage("Error executing: " + s).queue();
            }
        }, "LuaThread-" + String.format("%#s", event.getAuthor()));
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }
}
