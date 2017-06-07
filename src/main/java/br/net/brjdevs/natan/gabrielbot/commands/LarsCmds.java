package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.core.command.Argument;
import br.net.brjdevs.natan.gabrielbot.core.command.Command;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandCategory;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandPermission;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandReference;
import br.net.brjdevs.natan.gabrielbot.utils.Utils;
import br.net.brjdevs.natan.gabrielbot.utils.lua.InstructionLimitException;
import br.net.brjdevs.natan.gabrielbot.utils.lua.LuaHelper;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;

import java.io.PrintWriter;
import java.io.StringWriter;

public class LarsCmds {
    @Command(
            name = "larshelp",
            description = "Useful evals for lars",
            usage = "`>>larshelp member count`\n" +
                    "`>>larshelp bot count`\n" +
                    "`>>larshelp loops`\n" +
                    "`>>larshelp reactions`",
            permission = CommandPermission.LARS,
            category = CommandCategory.LARS
    )
    public static void larsHelp(@Argument("this") CommandReference thiz, @Argument("event")GuildMessageReceivedEvent event, @Argument("args") String[] args, @Argument("channel") TextChannel channel) {
        if (args.length == 0) {
            thiz.onHelp(event);
            return;
        }
        String s;
        switch (String.join(" ", args).toLowerCase()) {
            case "member count":
                s = "Eval JS: ```js\nreturn event.getGuild().getMembers().size()```";
                break;
            case "bot count":
                s = "Eval JS: ```js\nreturn event.getGuild().getMembers().stream().filter(function(m){return m.getUser().isBot();}).count()```";
                break;
            case "loops":
                s = "Eval BSH: ```java\nfor(int i = 0; i < NUMBER_OF_TIMES; i++){\n\t//code\n}```";
                break;
            case "reactions":
                s = "Eval JS: ```js\nvar t = Java.type(\"java.lang.Thread\"); new t(function(){event.getChannel().getHistory().retrievePast(100).complete().forEach(function(m){m.addReaction(\":eggplant:\").queue();});}).start()```";
                break;
            default:
                thiz.onHelp(event);
                return;
        }
        channel.sendMessage(s).queue();
    }

    @Command(
            name = "larseval",
            description = "Evaluates lua code",
            usage = "`>>larseval <code>`",
            permission = CommandPermission.LARS,
            category = CommandCategory.LARS,
            advancedSplit = false
    )
    public static void larsEval(@Argument("this") CommandReference thiz, @Argument("event") GuildMessageReceivedEvent event, @Argument("args") String[] args, @Argument("channel") TextChannel channel) {
        if (args.length == 0) {
            thiz.onHelp(event);
            return;
        }
        Globals globals = LuaHelper.setup(event, 20000, 5, "", null);
        String code = String.join(" ", args);
        try {
            LuaValue v = globals.load(code).call();
            if (!v.isnil()) {
                String toString;
                LuaValue tostring = globals.get("tostring");
                if (tostring.isfunction()) {
                    toString = tostring.call(v).tojstring();
                } else {
                    toString = v.tojstring();
                }
                if (toString.length() > 500) {
                    toString = Utils.paste(toString);
                } else {
                    toString = "```\n" + toString + "```";
                }
                channel.sendMessage("Evaluated successfully and returned " + toString).queue();
            } else {
                channel.sendMessage("Evaluated successfully with no returns").queue();
            }
        } catch (Throwable t) {
            if (t.getCause() instanceof InstructionLimitException) {
                channel.sendMessage("Cycle limit exceeded").queue();
                return;
            }
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.close();
            String s = sw.toString();
            if (s.length() > 500) {
                s = Utils.paste(s);
            } else {
                s = "```\n" + s + "```";
            }
            channel.sendMessage("Error executing: " + s).queue();
        }
    }
}
