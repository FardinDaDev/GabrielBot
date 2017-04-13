package br.net.brjdevs.natan.gabrielbot.core.command;

import br.net.brjdevs.natan.gabrielbot.utils.StringUtils;
import com.google.common.base.Preconditions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface SimpleCommand extends Command {
    @Override
    default void run(GuildMessageReceivedEvent event) {
        call(event, splitArgs(event));
    }

    void call(GuildMessageReceivedEvent event, String[] args);

    default String[] splitArgs(GuildMessageReceivedEvent event) {
        String[] parts = event.getMessage().getRawContent().split(" ", 2);
        if(parts.length == 1) return new String[0];
        return StringUtils.advancedSplitArgs(parts[1], 0);
    }

    static Builder builder() {
        return new Builder();
    }

    class Builder {
        private BiConsumer<GuildMessageReceivedEvent, String[]> code;
        private String description;
        private Message help;
        private CommandPermission permission;

        public Builder code(BiConsumer<GuildMessageReceivedEvent, String[]> code) {
            this.code = Preconditions.checkNotNull(code, "code");
            return this;
        }

        public Builder description(String description) {
            this.description = Preconditions.checkNotNull(description, "description");
            return this;
        }

        public Builder help(Message help) {
            this.help = Preconditions.checkNotNull(help, "help");
            return this;
        }

        public Builder help(MessageEmbed embed) {
            return help(new MessageBuilder().setEmbed(Preconditions.checkNotNull(embed, "embed")).build());
        }

        public Builder permission(CommandPermission permission) {
            this.permission = Preconditions.checkNotNull(permission);
            return this;
        }

        public Command build() {
            Preconditions.checkNotNull(code, "code");
            Preconditions.checkNotNull(permission, "permission");
            Preconditions.checkNotNull(description, "description");
            if(help == null)
                help = new MessageBuilder().append("No help available for this command").build();
            return new SimpleCommand() {
                @Override
                public void call(GuildMessageReceivedEvent event, String[] args) {
                    code.accept(event, args);
                }

                @Override
                public CommandPermission permission() {
                    return permission;
                }

                @Override
                public String description() {
                    return description;
                }

                @Override
                public Message help() {
                    return help;
                }
            };
        }
    }

    static MessageEmbed helpEmbed(String name, CommandPermission permission, String description, String usage) {
        String cmdname = Character.toUpperCase(name.charAt(0)) + name.substring(1) + " Command";
        String p = permission.name().toLowerCase();
        String perm = Character.toUpperCase(p.charAt(0)) + p.substring(1);
        return new EmbedBuilder()
                .setTitle(cmdname, null)
                .setDescription("\u200B")
                .addField("Permission required", perm, false)
                .addField("Description", description, false)
                .addField("Usage", usage, false)
                .build();
    }
}
