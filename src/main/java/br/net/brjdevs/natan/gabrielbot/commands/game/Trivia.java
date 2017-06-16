package br.net.brjdevs.natan.gabrielbot.commands.game;

import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import br.net.brjdevs.natan.gabrielbot.utils.OpenTriviaDatabase;
import gnu.trove.set.TLongSet;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Trivia extends Game {
    private final OpenTriviaDatabase.Question question;
    private int triesLeft;

    public Trivia(TextChannel channel, TLongSet players, OpenTriviaDatabase.Question question) {
        super(channel, players);
        this.question = question;

        List<String> options = new ArrayList<>(question.incorrectAnswers);
        options.add(question.correctAnswer);

        triesLeft = options.size()/2;

        Collections.shuffle(options);

        channel.sendMessage(new EmbedBuilder()
                .setDescription("**" + question.question + "**")
                .addField("Options", options.stream().collect(Collectors.joining("**\n**", "**", "**")), false)
                .addField("Difficulty", question.difficulty, true)
                .addField("Category", question.category, true)
                .setFooter(triesLeft + " tries left", null)
                .build()
        ).queue();
    }

    @Override
    protected boolean onMessage(GuildMessageReceivedEvent event) {
        String msg = event.getMessage().getContent();
        if(--triesLeft == 0) {
            say("Max number of attempts reached! The correct answer was `" + question.correctAnswer + "`");
            return true;
        }
        if(msg.replace("\\s+", " ").equalsIgnoreCase(question.correctAnswer)) {
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
}
