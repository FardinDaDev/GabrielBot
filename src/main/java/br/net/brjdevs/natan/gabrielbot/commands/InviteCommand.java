package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.core.command.Argument;
import br.net.brjdevs.natan.gabrielbot.core.command.Command;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandCategory;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandPermission;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;

public class InviteCommand {
    @Command(
            name = "invite",
            description = "Shows some of my links",
            usage = "`>>invite`",
            permission = CommandPermission.USER,
            category = CommandCategory.MISC
    )
    public static void register(@Argument("channel") TextChannel channel, @Argument("jda")JDA jda) {
        channel.sendMessage(
                new EmbedBuilder()
                        .setDescription("Here are some useful links! If you have any questions about the bot, feel free to join the support guild and ask!.\n" +
                                "We provided a patreon link in case you would like to help Gabriel keep running by donating [and getting perks by doing so!]. Thanks you in advance for using the bot! <3 from the developer\n\n[Invite URL](" +
                                String.format("https://discordapp.com/oauth2/authorize?client_id=%d&scope=bot&permissions=%d",
                                        jda.getSelfUser().getIdLong(),
                                        Permission.getRaw(Permission.ADMINISTRATOR)
                                ) + ")\n" +
                                "[Support Guild](https://discord.gg/pdnEXkd)\n" +
                                "[Patreon Page](https://www.patreon.com/gabrielbot)")
                        .build()
        ).queue();
    }
}
