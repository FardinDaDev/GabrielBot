package gabrielbot.utils.pokeapi;

import javax.annotation.Nonnull;

public class Ability implements Comparable<Ability> {
    public final String name;
    public final boolean hidden;

    Ability(String name, boolean hidden) {
        this.name = name;
        this.hidden = hidden;
    }

    @Override
    public int compareTo(@Nonnull Ability o) {
        return name.compareTo(o.name);
    }
}
