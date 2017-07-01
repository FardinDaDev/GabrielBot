package gabrielbot.utils.pokeapi;

import javax.annotation.Nonnull;

public class Stat implements Comparable<Stat> {
    public enum Type {
        HP("HP"), ATTACK("Attack"), DEFENSE("Defense"), SPECIAL_ATTACK("Sp. Attack"), SPECIAL_DEFENSE("Sp. Defense"), SPEED("Speed");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static Type fromName(String name) {
            return valueOf(name.toUpperCase().replace('-', '_'));
        }
    }

    public final Type type;
    public final int base;
    public final int effort;

    Stat(Type type, int base, int effort) {
        this.type = type;
        this.base = base;
        this.effort = effort;
    }

    Stat(String typeName, int base, int effort) {
        this(Type.fromName(typeName), base, effort);
    }

    @Override
    public int compareTo(@Nonnull Stat o) {
        return type.compareTo(o.type);
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
