package gabrielbot.utils.pokeapi;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import gabrielbot.utils.HTTPRequester;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;

public class PokeAPI {
    private static final HTTPRequester REQUESTER = new HTTPRequester("PokeAPI");

    private final LoadingCache<String, Optional<Pokemon>> pokemonCache;

    public PokeAPI(File storageDir) {
        storageDir.mkdirs();
        this.pokemonCache = Caffeine.newBuilder()
                .maximumSize(100)
                .build((k)->{
                    File f = new File(storageDir, k + ".pokemon");
                    if(f.exists()) {
                        FileInputStream fis = new FileInputStream(f);
                        Pokemon p = Pokemon.load(new DataInputStream(fis));
                        fis.close();
                        return Optional.of(p);
                    }
                    return Optional.ofNullable(fromJSONObject(f, REQUESTER.newRequest("http://pokeapi.co/api/v2/pokemon/" + k)
                            .header("User-Agent", "Gabriel (Discord bot)")
                            .get()
                            .asObject()
                    ));
                });
    }

    public Pokemon get(Integer identifier) {
        return pokemonCache.get(identifier.toString()).orElse(null);
    }

    public Pokemon get(String name) {
        String s = name.toLowerCase().trim();
        s = s.replaceAll("^0*", "");
        if(s.isEmpty()) return null;
        return pokemonCache.get(s).orElse(null);
    }

    private static Pokemon fromJSONObject(File toSave, JSONObject o) throws IOException {
        if(o.has("detail") /*not found*/ || o.has("next") /*pagination*/) return null;
        try {
            Pokemon p = Pokemon.load(o);
            FileOutputStream fos = new FileOutputStream(toSave);
            p.save(new DataOutputStream(fos));
            fos.close();
            return p;
        } catch(JSONException e) {
            return null;
        }
    }
}
