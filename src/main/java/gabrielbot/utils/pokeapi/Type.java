package gabrielbot.utils.pokeapi;

import java.awt.Color;

public enum Type {
    NORMAL(0xA8A878), FIRE(0xF08030), FIGHTING(0xF08030), WATER(0x6890F0), FLYING(0xA890F0), GRASS(0x78C850),
    POISON(0xA040A0), ELECTRIC(0xF8D030), GROUND(0xE0C068), PSYCHIC(0xF85888), ROCK(0xB8A038), ICE(0x98D8D8),
    BUG(0xA8B820), DRAGON(0x7038F8), GHOST(0x705898), DARK(0x705848), STEEL(0xB8B8D0), FAIRY(0xEE99AC),
    UNKNOWN(0x68A090) {
        @Override
        public int code() {
            return -1;
        }
    };

    private final Color color;

    Type(int rgb) {
        this.color = new Color(rgb);
    }

    public Color getColor() {
        return color;
    }

    @Override
    public String toString() {
        String n = name();
        return n.charAt(0) + n.toLowerCase().substring(1);
    }

    public static Type fromName(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch(EnumConstantNotPresentException e) {
            return UNKNOWN;
        }
    }

    public static Type fromCode(int code) {
        Type[] values = values();
        if(code < 0 || code > values.length) return UNKNOWN;
        return values[code];
    }

    public int code() {
        return ordinal();
    }
}
