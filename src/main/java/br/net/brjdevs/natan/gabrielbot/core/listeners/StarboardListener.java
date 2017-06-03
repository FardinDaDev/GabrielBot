package br.net.brjdevs.natan.gabrielbot.core.listeners;

import br.com.brjdevs.highhacks.eventbus.Listener;
import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import br.net.brjdevs.natan.gabrielbot.core.listeners.operations.ReactionOperation;
import br.net.brjdevs.natan.gabrielbot.core.listeners.operations.ReactionOperations;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent;
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

    private static final ExpiringMap<Long, Integer> LOGGED = ExpiringMap.builder()
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
            if(event.getUser().getIdLong() == m.getAuthor().getIdLong()) return;
            if(m.getCreationTime().plusSeconds(STARBOARD_MAX_AGE_SECONDS).isBefore(OffsetDateTime.now())) return;
            GabrielData.GuildData data = GabrielData.guilds().get().get(event.getGuild().getId());
            if(data == null || data.starboardChannelId == 0) return;
            TLongSet blacklist = data.starboardBlacklist;
            if(blacklist != null && blacklist.contains(event.getUser().getIdLong())) return;
            int minStars = data.minStars;
            Integer i = LOGGED.computeIfAbsent(m.getIdLong(), k->0)+1;
            if(i != minStars) {
                LOGGED.put(m.getIdLong(), i);
                return;
            }
            if(m.getChannel().getIdLong() == data.starboardChannelId) return;
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
                    .setFooter("Message sent on #" + event.getChannel().getName(), null)
                    .setTitle(String.format("%d %s | %d", 1, STAR_2, id));
            tc.sendMessage(eb.build()).queue(message->{
                ReactionOperations.create(event.getMessageIdLong(), STARBOARD_MAX_AGE_SECONDS, new StarOperation(message, id, m.getAuthor().getIdLong(), eb, blacklist, minStars));
            });
        }
    }

    private static class StarOperation implements ReactionOperation {
        private final Message starboardMessage;
        private final long messageId;
        private final long messageAuthor;
        private final EmbedBuilder builder;
        private final TLongSet blacklist;
        private final int minStars;
        private final AtomicInteger reactions = new AtomicInteger(1);

        StarOperation(Message starboardMessage, long messageId, long messageAuthor, EmbedBuilder builder, TLongSet blacklist, int minStars) {
            this.starboardMessage = starboardMessage;
            this.messageId = messageId;
            this.messageAuthor = messageAuthor;
            this.builder = builder;
            this.blacklist = blacklist == null ? new TLongHashSet() : blacklist;
            this.minStars = minStars;
        }

        @Override
        public boolean add(MessageReactionAddEvent event) {
            if(event.getUser().isBot() || event.getUser().getIdLong() == messageAuthor) return false;
            String reaction = event.getReactionEmote().getName();
            if(!reaction.equals(STAR_1)) return false;
            if(checkPerms(event)) return false;
            reactions.incrementAndGet();
            return update();
        }

        @Override
        public boolean remove(MessageReactionRemoveEvent event) {
            if(event.getUser().isBot() || event.getUser().getIdLong() == messageAuthor) return false;
            String reaction = event.getReactionEmote().getName();
            if(!reaction.equals(STAR_1)) return false;
            if(checkPerms(event)) return false;
            reactions.decrementAndGet();
            return update();
        }

        @Override
        public boolean removeAll(MessageReactionRemoveAllEvent event) {
            starboardMessage.delete().queue();
            return true;
        }

        private boolean checkPerms(GenericMessageReactionEvent event) {
            return blacklist.contains(event.getUser().getIdLong());
        }

        private boolean update() {
            int r = reactions.get();
            if(r < minStars) {
                starboardMessage.delete().queue();
                return true;
            }
            starboardMessage.editMessage(builder.setTitle(String.format("%d %s | %d", r, STAR_2, messageId)).build()).queue();
            return false;
        }
    }
}
