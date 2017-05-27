package br.net.brjdevs.natan.gabrielbot.core.command;

import br.com.brjdevs.java.utils.functions.TriConsumer;
import br.net.brjdevs.natan.gabrielbot.utils.StringUtils;
import com.google.common.base.Preconditions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;


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

    default EmbedBuilder helpEmbed(GuildMessageReceivedEvent event, String name) {
        String cmdname = Character.toUpperCase(name.charAt(0)) + name.substring(1) + " Command";
        String p = permission().name().toLowerCase();
        String perm = Character.toUpperCase(p.charAt(0)) + p.substring(1);
        return new EmbedBuilder()
                .setTitle(cmdname, null)
                .setDescription("\u200B")
                .addField("Permission required", perm, false)
                .setFooter("Requested by " + event.getAuthor().getName(), null);
    }

    default MessageEmbed helpEmbed(GuildMessageReceivedEvent event, String name, String usage) {
        return helpEmbed(event, name)
                .addField("Description", description(event), false)
                .addField("Usage", usage, false)
                .build();
    }

    default void onHelp(GuildMessageReceivedEvent event) {
        event.getChannel().sendMessage(help(event)).queue();
    }

    static Builder builder(CommandCategory category) {
        return new Builder(category);
    }

    class Builder {
        private final CommandCategory category;

        private Function<GuildMessageReceivedEvent, String[]> splitter;
        private TriConsumer<SimpleCommand, GuildMessageReceivedEvent, String[]> code;
        private BiFunction<SimpleCommand, GuildMessageReceivedEvent, MessageEmbed> help;
        private String description;
        private CommandPermission permission;
        private boolean hidden = false;

        public Builder(CommandCategory category) {
            this.category = category;
            this.permission = category.permissionRequired;
        }

        public Builder code(TriConsumer<SimpleCommand, GuildMessageReceivedEvent, String[]> code) {
            this.code = Preconditions.checkNotNull(code, "code");
            return this;
        }

        public Builder code(BiConsumer<GuildMessageReceivedEvent, String[]> code) {
            Preconditions.checkNotNull(code, "code");
            return code((ignored, event, args)->code.accept(event, args));
        }

        public Builder code(Consumer<GuildMessageReceivedEvent> code) {
            Preconditions.checkNotNull(code, "code");
            return code((ignored, event, args)->code.accept(event));
        }

        public Builder help(BiFunction<SimpleCommand, GuildMessageReceivedEvent, MessageEmbed> help) {
            this.help = Preconditions.checkNotNull(help, "help");
            return this;
        }
        public Builder description(String description) {
            this.description = Preconditions.checkNotNull(description, "description");
            return this;
        }

        public Builder permission(CommandPermission permission) {
            this.permission = Preconditions.checkNotNull(permission);
            return this;
        }

        public Builder splitter(Function<GuildMessageReceivedEvent, String[]> splitter) {
            this.splitter = splitter;
            return this;
        }

        public Builder hidden(boolean hidden) {
            this.hidden = hidden;
            return this;
        }

        public Command build() {
            Preconditions.checkNotNull(code, "code");
            Preconditions.checkNotNull(permission, "permission");
            Preconditions.checkNotNull(description, "description");
            if(help == null)
                help = (ignored1, ignored2)->new EmbedBuilder().setDescription("No help available for this command").build();
            return new SimpleCommand() {
                @Override
                public void call(GuildMessageReceivedEvent event, String[] args) {
                    code.accept(this, event, args);
                }

                @Override
                public CommandPermission permission() {
                    return permission;
                }

                @Override
                public String description(GuildMessageReceivedEvent event) {
                    return description;
                }

                @Override
                public MessageEmbed help(GuildMessageReceivedEvent event) {
                    return help.apply(this, event);
                }

                @Override
                public boolean isHiddenFromHelp() {
                    return hidden;
                }

                @Override
                public CommandCategory category() {
                    return category;
                }

                @Override
                public String[] splitArgs(GuildMessageReceivedEvent event) {
                    if(splitter == null)
                        return SimpleCommand.super.splitArgs(event);
                    return splitter.apply(event);
                }
            };
        }
    }
}
