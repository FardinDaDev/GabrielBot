package gabrielbot.commands;

import gabrielbot.commands.game.Pokemon;
import gabrielbot.commands.game.Trivia;
import gabrielbot.core.command.Argument;
import gabrielbot.core.command.Command;
import gabrielbot.core.command.CommandCategory;
import gabrielbot.core.command.CommandPermission;
import gabrielbot.core.listeners.operations.InteractiveOperations;
import gabrielbot.utils.OpenTriviaDatabase;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

@SuppressWarnings("unused")
public class GameCommands {
    @Command(
            name = "trivia",
            description = "Play trivia",
            usage = "`>>trivia`: Play trivia",
            permission = CommandPermission.USER,
            category = CommandCategory.GAME
    )
    public static void trivia(@Argument("channel") TextChannel channel, @Argument("author") User author) {
        if(check(channel)) return;
        OpenTriviaDatabase.Question q = OpenTriviaDatabase.random();
        if(q == null) {
            channel.sendMessage("Error getting a question from open trivia database").queue();
            return;
        }
        TLongSet players = new TLongHashSet();
        players.add(author.getIdLong());
        new Thread(()->{
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
                return;
            }
            InteractiveOperations.create(channel.getIdLong(), 120, new Trivia(channel, players, q));
        }).start();
    }

    @Command(
            name = "pokemonguess",
            description = "Guess which pokemon it is",
            usage = "`>>pokemonguess`: Play pokemon guess solo\n" +
                    "`>>pokemonguess @Someone @SomeoneElse ...`: Play pokemon guess with your friends (if you have any)",
            permission = CommandPermission.USER,
            category = CommandCategory.GAME
    )
    public static void pokemonguess(@Argument("channel") TextChannel channel, @Argument("author") User author, @Argument("message") Message message) {
        if(check(channel)) return;
        TLongSet players = new TLongHashSet();
        players.add(author.getIdLong());
        for(User u : message.getMentionedUsers()) players.add(u.getIdLong());
        new Thread(()->{
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
                return;
            }
            InteractiveOperations.create(channel.getIdLong(), 120, new Pokemon(channel, players));
        }).start();
    }

    private static boolean check(TextChannel channel) {
        if(InteractiveOperations.get(channel) != null) {
            channel.sendMessage("There is already a game running on this channel").queue();
            return true;
        }
        return false;
    }
}
