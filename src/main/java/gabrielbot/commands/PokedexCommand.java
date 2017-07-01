package gabrielbot.commands;

import gabrielbot.GabrielBot;
import gabrielbot.core.command.Argument;
import gabrielbot.core.command.Command;
import gabrielbot.core.command.CommandCategory;
import gabrielbot.core.command.CommandPermission;
import gabrielbot.utils.Utils;
import gabrielbot.utils.pokeapi.Pokemon;
import gabrielbot.utils.pokeapi.Type;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.Arrays;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class PokedexCommand {
    @Command(
            name = "pokedex",
            description = "Looks info about a pokemon",
            usage = "`>>pokedex <name>`: Looks up a pokemon by name\n" +
                    "`>>pokedex <id>:` Looks up a pokemon by id",
            permission = CommandPermission.USER,
            category = CommandCategory.GAME
    )
    public static void pokedex(@Argument("channel") TextChannel channel, @Argument("args") String[] args) {
        if(args.length == 0) {
            channel.sendMessage("You need to say what I should look for!").queue();
            return;
        }
        Pokemon p = GabrielBot.getInstance().pokeapi.get(args[0]);
        if(p == null) {
            channel.sendMessage("I didn't find that pokemon").queue();
            return;
        }
        Type[] types = p.getTypes();
        String moves = p.getMoves().stream().collect(Collectors.joining("`, `", "`", "`"));
        if(moves.length() > 800) {
            moves = Utils.paste(p.getMoves().stream().collect(Collectors.joining(", ")));
        }
        channel.sendMessage(new EmbedBuilder()
                .setThumbnail(p.getImage())
                .setColor(types[0].getColor())
                .setTitle(p.getName())
                .addField("Pokedex ID", ""+p.getPokedex(), false)
                .addField(types.length == 1 ? "Type" : "Types", Arrays.stream(types).map(Type::toString).collect(Collectors.joining(", ")), false)
                .addField("Stats", Arrays.stream(p.getStats()).map(s->String.format("**%s**: Base %d, Effort %d", s, s.base, s.effort)).collect(Collectors.joining("\n")), false)
                .addField("Abilities", p.getAbilities().stream().map(a->"`" + a.name + "`" + (a.hidden ? "(hidden)" : "")).collect(Collectors.joining(", ")), false)
                .addField("Moves", moves, false)
                .build()).queue();
    }
}
