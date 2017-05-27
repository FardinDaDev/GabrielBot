package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.core.command.CommandCategory;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandRegistry;
import br.net.brjdevs.natan.gabrielbot.core.command.RegisterCommand;
import br.net.brjdevs.natan.gabrielbot.core.command.SimpleCommand;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

@RegisterCommand.Class
public class InviteCommand {
    @RegisterCommand
    public static void register(CommandRegistry cr) {
        cr.register("invite", SimpleCommand.builder(CommandCategory.MISC)
                .description("Shows my invite link!")
                .help((thiz, event)->thiz.helpEmbed(event, "invite", "`>>invite`"))
                .code((event, args)->{
                    event.getChannel().sendMessage(
                            new EmbedBuilder()
                            .setDescription("Here are some useful links! If you have any questions about the bot, feel free to join the support guild and ask!.\n" +
                                    "We provided a patreon link in case you would like to help Gabriel keep running by donating [and getting perks by doing so!]. Thanks you in advance for using the bot! <3 from the developer\n\n[Invite URL](" + String.format("https://discordapp.com/oauth2/authorize?client_id=%d&scope=bot&permissions=%d",
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
                                    )) + ")\n" +
                                    "[Support Guild](https://discord.gg/pdnEXkd)\n" +
                                    "[Patreon Page](https://www.patreon.com/gabrielbot)")
                            .build()
                    ).queue();
                })
                .build()
        );
    }
}
