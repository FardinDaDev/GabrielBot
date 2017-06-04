package br.net.brjdevs.natan.gabrielbot.commands.custom;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.impl.MessageEmbedImpl;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EmbedCustomCommand extends CustomCommand {
    private static final Map<String, Color> COLORS = new HashMap<>();

    private final String json;
    private transient volatile JSONObject jsonObj;

    static {
        for(Field f : Color.class.getFields()) {
            if(!Modifier.isStatic(f.getModifiers()) || f.getType() != Color.class) continue;
            try {
                COLORS.put(f.getName(), (Color)f.get(null));
            } catch(IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    public EmbedCustomCommand(String json) {
        try {
            jsonObj = new JSONObject(json);
        } catch(JSONException e) {
            json = "{" + json + "}";
            jsonObj = new JSONObject(json);
        }
        this.json = json;
    }

    @Override
    public String process(GuildMessageReceivedEvent event, String input, Map<String, String> mappings) {
        if(jsonObj == null) {
            synchronized(this) {
                if(jsonObj == null) {
                    jsonObj = new JSONObject(json);
                }
            }
        }
        MessageEmbedImpl embed = new MessageEmbedImpl();
        MessageEmbed.Footer[] footer = {new MessageEmbed.Footer("", null, null)};
        MessageEmbed.AuthorInfo[] author = {new MessageEmbed.AuthorInfo("", null, null, null)};
        List<MessageEmbed.Field> fields = new LinkedList<>();
        jsonObj.toMap().forEach((key, value)->{
            String v = map(String.valueOf(value), mappings);
            switch(key.toLowerCase()) {
                case "title":
                    embed.setTitle(v);
                    break;
                case "title_url":
                    embed.setUrl(v);
                    break;
                case "description":
                    embed.setDescription(v);
                    break;
                case "fields":
                    if(value instanceof Iterable) {
                        for(Object o : ((Iterable<?>) value)) {
                            if(o instanceof Map) {
                                Map<?,?> j = (Map<?,?>)o;
                                boolean inline = j.containsKey("inline") && j.get("inline") instanceof Boolean && (Boolean)j.get("inline");
                                String name = j.containsKey("name") ? String.valueOf(j.get("name")) : "\u200E";
                                String va = j.containsKey("value") ? String.valueOf(j.get("value")) : "\u200E";
                                fields.add(new MessageEmbed.Field(name, va, inline));
                            }
                        }
                    }
                    break;
                case "footer":
                    footer[0] = new MessageEmbed.Footer(v, footer[0].getIconUrl(), null);
                    break;
                case "footer_url":
                    footer[0] = new MessageEmbed.Footer(footer[0].getText(), v, null);
                    break;
                case "author":
                    author[0] = new MessageEmbed.AuthorInfo(v, author[0].getUrl(), author[0].getIconUrl(), null);
                    break;
                case "author_url":
                    author[0] = new MessageEmbed.AuthorInfo(author[0].getName(), v, author[0].getIconUrl(), null);
                    break;
                case "author_icon":
                    author[0] = new MessageEmbed.AuthorInfo(author[0].getName(), author[0].getUrl(), v, null);
                    break;
                case "image":
                    embed.setImage(new MessageEmbed.ImageInfo(v, null, 0, 0));
                    break;
                case "thumbnail":
                    embed.setThumbnail(new MessageEmbed.Thumbnail(v, null, 0, 0));
                    break;
                case "color":
                    Color c = COLORS.get(v);
                    if(c == null) {
                        if (v.equals("member")) {
                            c = event.getMember().getColor();
                        } else if (v.matches("(#|0x)?[0123456789abcdef]{1,6}")) {
                            try {
                                c = Color.decode(v.startsWith("0x") || v.startsWith("#") ? v : "0x" + v);
                            } catch (Exception ignored2) {}
                        }
                    }
                    embed.setColor(c);
                    break;
            }
        });
        embed.setFooter(footer[0].getText() == null ? null : footer[0]);
        embed.setAuthor(author[0].getName() == null ? null : author[0]);
        embed.setFields(fields);

        event.getChannel().sendMessage(embed).queue();
        return null;
    }

    @Override
    public String getRaw() {
        return json;
    }

}
