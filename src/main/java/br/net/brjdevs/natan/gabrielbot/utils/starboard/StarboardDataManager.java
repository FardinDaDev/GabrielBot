package br.net.brjdevs.natan.gabrielbot.utils.starboard;

import br.net.brjdevs.natan.gabrielbot.utils.data.JedisDataManager;
import net.dv8tion.jda.core.entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.util.List;

public class StarboardDataManager extends JedisDataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Starboard");

    public StarboardDataManager(JedisPool pool, String prefix) {
        super(pool, prefix);
    }

    public StarboardDataManager(String host, int port, String prefix) {
        super(host, port, prefix);
    }

    public StarboardDataManager(String host, int port) {
        super(host, port);
    }

    public long getStarboardMessage(long guildId, long messageId) {
        return run(j->{
            List<String> list = j.lrange(prefix + guildId, messageId, messageId);
            if(list.isEmpty()) return 0L;
            return Long.parseLong(list.get(0));
        });
    }

    public long getStarboardMessage(Message message) {
        return getStarboardMessage(message.getGuild().getIdLong(), message.getIdLong());
    }

    public void setStarboardMessage(long guildId, long messageId, long starboardMessageId) {
        runNoReply(j->{
            String s = j.lset(prefix + guildId, messageId, String.valueOf(starboardMessageId));
            if(s != null && !s.equals(String.valueOf(starboardMessageId))) {
                LOGGER.error("Duplicate star addition for message {} at guild {}", messageId, guildId);
            }
        });
    }

    public void setStarboardMessage(Message message, Message starboardMessage) {
        setStarboardMessage(message.getGuild().getIdLong(), message.getIdLong(), starboardMessage.getIdLong());
    }

    public void remove(long guildId, long starboardMessageId) {
        runNoReply(j->{
            j.lrem(prefix + guildId, 0, String.valueOf(starboardMessageId));
        });
    }

    public void remove(Message starboardMessage) {
        remove(starboardMessage.getGuild().getIdLong(), starboardMessage.getIdLong());
    }
}
