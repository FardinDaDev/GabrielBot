package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.core.command.CommandCategory;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandRegistry;
import br.net.brjdevs.natan.gabrielbot.core.command.RegisterCommand;
import br.net.brjdevs.natan.gabrielbot.core.command.SimpleCommand;
import net.dv8tion.jda.core.Permission;

@RegisterCommand.Class
public class InviteCommand {
    @RegisterCommand
    public static void register(CommandRegistry cr) {
        cr.register("invite", SimpleCommand.builder(CommandCategory.MISC)
                .description("invite", "Shows my invite link!")
                .help((thiz, event)->thiz.helpEmbed(event, "invite", "`>>invite`"))
                .code((event, args)->{
                    event.getChannel().sendMessage(
                            String.format("https://discordapp.com/oauth2/authorize?client_id=%d&scope=bot&permissions=%d",
                                    event.getJDA().getSelfUser().getIdLong(),
                                    Permission.getRaw(
                                            Permission.MESSAGE_READ,
                                            Permission.MESSAGE_WRITE,
                                            Permission.MESSAGE_EMBED_LINKS,
                                            Permission.BAN_MEMBERS,
                                            Permission.KICK_MEMBERS,
                                            Permission.MESSAGE_MANAGE,
                                            Permission.MESSAGE_ATTACH_FILES,
                                            Permission.VOICE_CONNECT,
                                            Permission.VOICE_SPEAK
                                    ))
                    ).queue();
                })
                .build()
        );
    }
}
