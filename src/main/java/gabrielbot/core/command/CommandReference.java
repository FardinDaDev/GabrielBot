package gabrielbot.core.command;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.lang.reflect.Method;

public class CommandReference {
    public final Command command;
    final CommandInvoker invoker;

    CommandReference(Command command, Method method, ClassLoader loader) {
        this.command = command;
        this.invoker = ASM.invoker(method, loader);
    }

    public void onHelp(GuildMessageReceivedEvent event) {
        String name = command.name();
        String cmdname = Character.toUpperCase(name.charAt(0)) + name.substring(1) + " Command";
        String p = command.permission().name().toLowerCase();
        String perm = Character.toUpperCase(p.charAt(0)) + p.substring(1);
        event.getChannel().sendMessage(new EmbedBuilder()
                .setColor(event.getMember().getColor())
                .setTitle(cmdname, null)
                .setDescription("\u200B")
                .addField("Permission required", perm, false)
                .setFooter("Requested by " + event.getAuthor().getName(), null)
                .addField("Description", command.description(), false)
                .addField("Usage", command.usage(), false).build()).queue();
    }
}
