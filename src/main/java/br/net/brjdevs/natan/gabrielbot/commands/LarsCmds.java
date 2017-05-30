package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.core.command.CommandCategory;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandRegistry;
import br.net.brjdevs.natan.gabrielbot.core.command.RegisterCommand;
import br.net.brjdevs.natan.gabrielbot.core.command.SimpleCommand;

@RegisterCommand.Class
public class LarsCmds {
    @RegisterCommand
    public static void larsHelp(CommandRegistry cr) {
        cr.register("larshelp", SimpleCommand.builder(CommandCategory.LARS)
                .description("Useful evals for Lars")
                .help((thiz, event)->thiz.helpEmbed(event, "larshelp",
                        "`>>larshelp member count`\n`>>larshelp bot count`\n`>>larshelp loops`\n`>>larshelp reactions`"
                ))
                .code((thiz, event, args)->{
                    if(args.length == 0) {
                        thiz.onHelp(event);
                        return;
                    }
                    String s;
                    switch(String.join(" ", args).toLowerCase()) {
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
                    event.getChannel().sendMessage(s).queue();
                })
                .build()
        );
    }

    @RegisterCommand
    public static void larsEval(CommandRegistry cr) {

    }
}
