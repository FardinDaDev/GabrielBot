package gabrielbot.commands.game;

import gabrielbot.core.data.GabrielData;
import gnu.trove.set.TLongSet;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Guess extends Game {
    private final String answer;
    private int triesLeft;

    public Guess(TextChannel channel, TLongSet players, String answer, int tries) {
        super(channel, players);
        this.answer = answer.replaceAll("\\s+", " ");
        this.triesLeft = tries;
    }

    @Override
    public int run(GuildMessageReceivedEvent event) {
        if(!players.contains(event.getAuthor().getIdLong())) return IGNORED;
        if(event.getMessage().getRawContent().equals("end")) {
            say("Ended game! Correct answer was `" + answer + "`");
            return COMPLETED;
        }
        return onMessage(event) ? COMPLETED : RESET_TIMEOUT;
    }

    @Override
    protected boolean onMessage(GuildMessageReceivedEvent event) {
        String msg = event.getMessage().getContent();
        if(triesLeft == 0) {
            say("Max number of attempts reached! The correct answer was `" + answer + "`");
            return true;
        }
        triesLeft--;
        if(msg.replace("\\s+", " ").equalsIgnoreCase(answer)) {
            say("Correct! You won 50 credits");
            GabrielData.UserData user = GabrielData.users().get().get(event.getAuthor().getId());
            if(user == null) {
                GabrielData.users().get().set(event.getAuthor().getId(), user = new GabrielData.UserData());
            }
            user.money += 50;
            return true;
        }
        say("Wrong option! " + triesLeft + " tries remaining");
        return false;
    }

    @Override
    public void onExpire() {
        say("Time over! The correct answer was `" + answer + "`");
    }
}
