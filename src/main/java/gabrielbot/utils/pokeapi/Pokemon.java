package gabrielbot.utils.pokeapi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Pokemon {
    private int pokedex;
    private String name;
    private Type[] types;
    private Stat[] stats;
    private List<Ability> abilities;
    private List<String> moves;

    public int getPokedex() {
        return pokedex;
    }

    public String getName() {
        return name;
    }

    public Type[] getTypes() {
        return types;
    }

    public Stat[] getStats() {
        return stats;
    }

    public List<Ability> getAbilities() {
        return abilities;
    }

    public List<String> getMoves() {
        return moves;
    }

    public String getImage() {
        return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/" + pokedex + ".png";
    }

    public static Pokemon load(DataInput input) throws IOException {
        Pokemon p = new Pokemon();
        p.pokedex = input.readInt();
        p.name = input.readUTF();
        byte b = input.readByte();
        p.types = new Type[b];
        for(byte i = 0; i < b; i++) {
            p.types[i] = Type.fromCode(input.readByte());
        }
        p.stats = new Stat[6];
        for(int i = 0; i < 6; i++) {
            p.stats[i] = new Stat(Stat.Type.values()[i], input.readInt(), input.readInt());
        }
        int i = input.readShort();
        p.abilities = new ArrayList<>();
        for(int j = 0; j < i; j++) {
            p.abilities.add(new Ability(input.readUTF(), input.readBoolean()));
        }
        i = input.readShort();
        p.moves = new ArrayList<>();
        for(int j = 0; j < i; j++) {
            p.moves.add(input.readUTF());
        }
        return p;
    }

    public static Pokemon load(JSONObject object) {
        Pokemon p = new Pokemon();
        p.pokedex = object.getInt("id");

        String n = object.getString("name");
        n = Character.toUpperCase(n.charAt(0)) + n.substring(1);
        p.name = n;

        JSONArray t = object.getJSONArray("types");
        Type[] types = new Type[t.length()];
        for(int i = 0; i < types.length; i++) {
            JSONObject type = t.getJSONObject(i);
            types[type.getInt("slot")-1] = Type.fromName(type.getJSONObject("type").getString("name"));
        }
        p.types = types;

        Stat[] stats = new Stat[6];
        JSONArray s = object.getJSONArray("stats");
        for(int i = 0; i < s.length(); i++) {
            JSONObject o = s.getJSONObject(i);
            String name = o.getJSONObject("stat").getString("name");
            Stat.Type type = Stat.Type.fromName(name);
            stats[type.ordinal()] = new Stat(type, o.getInt("base_stat"), o.getInt("effort"));
        }
        p.stats = stats;

        JSONArray a = object.getJSONArray("abilities");
        List<Ability> abilities = new ArrayList<>();
        for(int i = 0; i < a.length(); i++) {
            JSONObject ability = a.getJSONObject(i);
            String name = ability.getJSONObject("ability").getString("name");
            name = name.replace('-', ' ');
            StringBuilder nm = new StringBuilder();
            boolean shouldBeUpper = true;
            for(char c : name.toCharArray()) {
                if(shouldBeUpper) {
                    nm.append(Character.toUpperCase(c));
                    shouldBeUpper = false;
                    continue;
                }
                if(c == ' ') shouldBeUpper = true;
                nm.append(c);
            }
            abilities.add(new Ability(nm.toString(), ability.getBoolean("is_hidden")));
        }
        Collections.sort(abilities);
        p.abilities = abilities;

        JSONArray m = object.getJSONArray("moves");
        List<String> moves = new ArrayList<>();
        for(int i = 0; i < m.length(); i++) {
            String name = m.getJSONObject(i).getJSONObject("move").getString("name");
            name = name.replace('-', ' ');
            StringBuilder nm = new StringBuilder();
            boolean shouldBeUpper = true;
            for(char c : name.toCharArray()) {
                if(shouldBeUpper) {
                    nm.append(Character.toUpperCase(c));
                    shouldBeUpper = false;
                    continue;
                }
                if(c == ' ') shouldBeUpper = true;
                nm.append(c);
            }
            moves.add(nm.toString());
        }
        p.moves = moves;

        return p;
    }

    public void save(DataOutput output) throws IOException {
        output.writeInt(pokedex);
        output.writeUTF(name);
        output.writeByte(types.length);
        for(Type t : types) {
            output.writeByte(t.code());
        }
        for(Stat stat : stats) {
            output.writeInt(stat.base);
            output.writeInt(stat.effort);
        }
        output.writeShort(abilities.size());
        for(Ability a : abilities) {
            output.writeUTF(a.name);
            output.writeBoolean(a.hidden);
        }
        output.writeShort(moves.size());
        for(String s : moves) {
            output.writeUTF(s);
        }
    }
}
