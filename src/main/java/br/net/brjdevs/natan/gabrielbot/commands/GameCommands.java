package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.commands.game.Trivia;
import br.net.brjdevs.natan.gabrielbot.core.command.Argument;
import br.net.brjdevs.natan.gabrielbot.core.command.Command;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandCategory;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandPermission;
import br.net.brjdevs.natan.gabrielbot.core.listeners.operations.InteractiveOperations;
import br.net.brjdevs.natan.gabrielbot.utils.OpenTriviaDatabase;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

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

    private static boolean check(TextChannel channel) {
        if(InteractiveOperations.get(channel) != null) {
            channel.sendMessage("There is already a game running on this channel").queue();
            return true;
        }
        return false;
    }
}
