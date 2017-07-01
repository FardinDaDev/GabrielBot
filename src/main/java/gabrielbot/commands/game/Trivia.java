package gabrielbot.commands.game;

import gabrielbot.utils.OpenTriviaDatabase;
import gnu.trove.set.TLongSet;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Trivia extends Guess {
    public Trivia(TextChannel channel, TLongSet players, OpenTriviaDatabase.Question question) {
        super(channel, players, question.correctAnswer, tries(channel, question));
    }

    private static int tries(TextChannel channel, OpenTriviaDatabase.Question question) {
        List<String> options = new ArrayList<>(question.incorrectAnswers);
        options.add(question.correctAnswer);

        int triesLeft = options.size()/2;

        Collections.shuffle(options);

        channel.sendMessage(new EmbedBuilder()
                .setDescription("**" + question.question + "**")
                .addField("Options", options.stream().collect(Collectors.joining("**\n**", "**", "**")), false)
                .addField("Difficulty", question.difficulty, true)
                .addField("Category", question.category, true)
                .setFooter(triesLeft + " tries left", null)
                .build()
        ).queue();

        return triesLeft;
    }
}
