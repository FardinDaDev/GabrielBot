package br.net.brjdevs.natan.gabrielbot.core.listeners;

import br.com.brjdevs.highhacks.eventbus.Listener;
import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import br.net.brjdevs.natan.gabrielbot.core.listeners.operations.ReactionOperation;
import br.net.brjdevs.natan.gabrielbot.core.listeners.operations.ReactionOperations;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class StarboardListener implements EventListener {
    public static final String STAR_1 = "\u2b50", STAR_2 = "\uD83C\uDF1F";

    public static final long STARBOARD_MAX_AGE_SECONDS = 600;

    private static final ExpiringMap<Long, Void> LOGGED = ExpiringMap.builder()
            .expirationPolicy(ExpirationPolicy.CREATED)
            .expiration(STARBOARD_MAX_AGE_SECONDS + (long)(STARBOARD_MAX_AGE_SECONDS * 0.05), TimeUnit.SECONDS)
            .build();

    @Override
    @Listener
    public void onEvent(Event e) {
        if(e instanceof MessageReactionAddEvent) {
            MessageReactionAddEvent event = (MessageReactionAddEvent)e;
            if(event.getUser().isBot()) return;
            if(!event.isFromType(ChannelType.TEXT)) return;
            String reaction = event.getReactionEmote().getName();
            if(!reaction.equals(STAR_1)) return;
            long id = event.getMessageIdLong();
            Message m = event.getChannel().getMessageById(id).complete();
            if(m.getCreationTime().plusSeconds(STARBOARD_MAX_AGE_SECONDS).isBefore(OffsetDateTime.now())) return;
            if(LOGGED.containsKey(id)) return;
            LOGGED.put(id, null);
            GabrielData.GuildData data = GabrielData.guilds().get().get(event.getGuild().getId());
            if(data == null || data.starboardChannelId == 0) return;
            TextChannel tc = event.getGuild().getTextChannelById(data.starboardChannelId);
            if(tc == null || !tc.canTalk()) {
                if (event.getTextChannel().canTalk()) {
                    event.getChannel().sendMessage("Unable to add message to starboard, check that the configured channel exists and I can talk there").queue();
                }
                data.starboardChannelId = 0;
                return;
            }
            String content = m.getRawContent();
            EmbedBuilder eb = new EmbedBuilder()
                    .setDescription(content)
                    .setAuthor(m.getAuthor().getName() + "#" + m.getAuthor().getDiscriminator(), null, m.getAuthor().getEffectiveAvatarUrl())
                    .setTimestamp(m.getCreationTime())
                    .setFooter("Message sent on channel " + event.getChannel().getName(), null)
                    .setTitle(String.format("%d %s | %d", 1, STAR_2, id));
            tc.sendMessage(eb.build()).queue(message->{
                ReactionOperations.create(event.getMessageIdLong(), 60, new StarOperation(message, id, eb));
            });
        }
    }

    private static class StarOperation implements ReactionOperation {
        private final Message starboardMessage;
        private final long messageId;
        private final EmbedBuilder builder;
        private final AtomicInteger reactions = new AtomicInteger(1);

        StarOperation(Message starboardMessage, long messageId, EmbedBuilder builder) {
            this.starboardMessage = starboardMessage;
            this.messageId = messageId;
            this.builder = builder;
        }

        @Override
        public boolean add(MessageReactionAddEvent event) {
            if(event.getUser().isBot()) return false;
            String reaction = event.getReactionEmote().getName();
            if(!reaction.equals(STAR_1)) return false;
            reactions.incrementAndGet();
            return update();
        }

        @Override
        public boolean remove(MessageReactionRemoveEvent event) {
            if(event.getUser().isBot()) return false;
            String reaction = event.getReactionEmote().getName();
            if(!reaction.equals(STAR_1)) return false;
            reactions.decrementAndGet();
            return update();
        }

        @Override
        public boolean removeAll(MessageReactionRemoveAllEvent event) {
            starboardMessage.delete().queue();
            return true;
        }

        private boolean update() {
            int r = reactions.get();
            if(r == 0) {
                starboardMessage.delete().queue();
                return true;
            }
            starboardMessage.editMessage(builder.setTitle(String.format("%d %s | %d", r, STAR_2, messageId)).build()).queue();
            return false;
        }
    }
}
