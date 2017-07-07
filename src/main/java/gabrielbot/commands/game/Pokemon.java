package gabrielbot.commands.game;

import gabrielbot.utils.Randoms;
import gabrielbot.utils.data.JarTextFileDataManager;
import gnu.trove.set.TLongSet;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.List;

public class Pokemon extends Guess {
    private static final List<String> options = new JarTextFileDataManager("/assets/pokemon.txt").get();

    public Pokemon(TextChannel channel, TLongSet players) {
        super(channel, players, setup(channel), 5);
    }

    private static String setup(TextChannel channel) {
        String s = options.get(Randoms.nextInt(options.size()));
        String[] parts = s.split("`");
        String url = parts[0];

        channel.sendMessage(new EmbedBuilder()
            .setImage(url)
            .setTitle("Who's that pokemon?")
            .build()
        ).queue();

        return parts[1];
    }
}
