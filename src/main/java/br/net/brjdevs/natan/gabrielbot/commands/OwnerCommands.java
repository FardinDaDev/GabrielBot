package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.core.command.Argument;
import br.net.brjdevs.natan.gabrielbot.core.command.Command;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandCategory;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandPermission;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandReference;
import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import br.net.brjdevs.natan.gabrielbot.music.SerializedPlayer;
import br.net.brjdevs.natan.gabrielbot.music.SerializedTrack;
import br.net.brjdevs.natan.gabrielbot.music.Track;
import br.net.brjdevs.natan.gabrielbot.utils.DumpUtils;
import br.net.brjdevs.natan.gabrielbot.utils.KryoUtils;
import br.net.brjdevs.natan.gabrielbot.utils.UnsafeUtils;
import br.net.brjdevs.natan.gabrielbot.utils.Utils;
import br.net.brjdevs.natan.gabrielbot.utils.data.JedisDataManager;
import br.net.brjdevs.natan.gabrielbot.utils.lua.LuaHelper;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class OwnerCommands {
    private static String imports = "package dontuse;\n";

    static {
        addImport("java.util.*");
        addImport("java.net.*");
        addImport("java.math.*");
        addImport("java.io.*");
        addImport("java.util.function.*");
        addImport("java.util.stream.*");
        addImport("java.lang.reflect.*");

        addImport("net.dv8tion.jda.core.*");
        addImport("net.dv8tion.jda.core.entities.*");
        addImport("net.dv8tion.jda.core.requests.*");
        addImport("net.dv8tion.jda.core.managers.*");
        addImport("net.dv8tion.jda.core.utils.*");
        addImport("net.dv8tion.jda.core.events.*");
        addImport("net.dv8tion.jda.core.events.message.*");
        addImport("net.dv8tion.jda.core.events.message.guild.*");

        addImport("br.net.brjdevs.natan.gabrielbot.*");
        addImport("br.net.brjdevs.natan.gabrielbot.commands.*");
        addImport("br.net.brjdevs.natan.gabrielbot.core.command.*");
        addImport("br.net.brjdevs.natan.gabrielbot.commands.custom.*");
        addImport("br.net.brjdevs.natan.gabrielbot.core.data.*");
        addImport("br.net.brjdevs.natan.gabrielbot.core.jda.*");
        addImport("br.net.brjdevs.natan.gabrielbot.core.listeners.*");
        addImport("br.net.brjdevs.natan.gabrielbot.log.*");
        addImport("br.net.brjdevs.natan.gabrielbot.music.*");
        addImport("br.net.brjdevs.natan.gabrielbot.utils.*");
        addImport("br.net.brjdevs.natan.gabrielbot.utils.data.*");
        addImport("br.net.brjdevs.natan.gabrielbot.utils.cache.*");

        addImport("br.com.brjdevs.highhacks.eventbus.*");
    }

    public static void addImport(String name) {
        imports += "import " + name + ";\n";
    }

    @Command(
            name = "save",
            description = "FLushes data to db",
            usage = "`>>save`",
            permission = CommandPermission.OWNER,
            category = CommandCategory.OWNER
    )
    public static void save(@Argument("this") CommandReference thiz, @Argument("event")GuildMessageReceivedEvent event, @Argument("args") String[] args) {
        GabrielData.save();
        event.getChannel().sendMessage("Successfully saved").queue();
    }

    @Command(
            name = "premium",
            description = "Gives premium to an user/guild",
            usage = "`>>premium user/guild <id> <days>`\n\n(give 0 to remove)",
            permission = CommandPermission.OWNER,
            category = CommandCategory.OWNER
    )
    public static void premium(@Argument("this") CommandReference thiz, @Argument("event")GuildMessageReceivedEvent event, @Argument("args") String[] args) {
        if(args.length < 3) {
            thiz.onHelp(event);
            return;
        }
        long id, time;
        try {
            id = Long.parseLong(args[1]);
            time = TimeUnit.DAYS.toMillis(Long.parseLong(args[2]));
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage(e.getMessage() + ": not a valid number").queue();
            return;
        }
        switch(args[0]) {
            case "user":
                GabrielData.UserData user = GabrielData.users().get().get(String.valueOf(id));
                if(user == null) {
                    GabrielData.users().get().set(String.valueOf(id), user = new GabrielData.UserData());
                }
                user.premiumUntil = time == 0 ? 0 : System.currentTimeMillis() + time;
                break;
            case "guild":
                GabrielData.GuildData guild = GabrielData.guilds().get().get(String.valueOf(id));
                if(guild == null) {
                    GabrielData.guilds().get().set(String.valueOf(id), guild = new GabrielData.GuildData());
                }
                guild.premiumUntil = time == 0 ? 0 : System.currentTimeMillis() + time;
                break;
            default:
                thiz.onHelp(event);
        }
    }

    @Command(
            name = "shutdown",
            description = "Makes me go offline :(",
            usage = "`>>shutdown`\n" +
                    "`>>shutdown savemusic`",
            permission = CommandPermission.OWNER,
            category = CommandCategory.OWNER
    )
    public static void shutdown(@Argument("this") CommandReference thiz, @Argument("event")GuildMessageReceivedEvent event, @Argument("args") String[] args) {
        if (args.length > 0 && args[0].equals("savemusic")) {
            GabrielBot.getInstance().registry.commands().remove("play");
            JedisDataManager jdm = (JedisDataManager) GabrielData.guilds();
            int[] i = new int[1];
            GabrielBot.getInstance().streamPlayers().forEach(p -> {
                p.player.setPaused(true);
                List<Track> allTracks = new ArrayList<>();
                allTracks.add(p.scheduler.currentTrack());
                allTracks.addAll(p.scheduler.tracks());
                p.leave();
                p.getTextChannel().sendMessage("I'll be rebooting soon, but your queue has been saved and will be restored after I reboot").queue();
                SerializedPlayer sp = new SerializedPlayer(allTracks.stream().map(SerializedTrack::new).collect(Collectors.toList()), allTracks.get(0).track.getPosition(), p.guildId, p.textChannelId, p.voiceChannelId);
                jdm.set("p_" + p.guildId, Base64.getEncoder().encodeToString(KryoUtils.serialize(sp)));
                i[0]++;
            });
            jdm.save();
            GabrielBot.LOGGER.info("Serialized tracks for {} guilds", i[0]);
        }
        GabrielData.save();
        event.getChannel().sendMessage("*Goes to sleep...*").complete();
        Arrays.stream(GabrielBot.getInstance().getShards()).forEach(s -> s.getJDA().shutdown(true));
        System.exit(0);
    }

    @Command(
            name = "dump",
            description = "Dumps threads or heap to a file",
            usage = "`>>dump heap <file>`: Dumps the heap to the given file\n" +
                    "`>>dump threads <file>`: Dumps the threads to the given file",
            permission = CommandPermission.OWNER,
            category = CommandCategory.OWNER
    )
    public static void dump(@Argument("this") CommandReference thiz, @Argument("event")GuildMessageReceivedEvent event, @Argument("args") String[] args) {
        if(args.length < 2) {
            thiz.onHelp(event);
            return;
        }
        File file = new File(args[1]);
        switch(args[0]) {
            case "heap":
                try {
                    DumpUtils.dumpHeap(args[1]);
                    event.getChannel().sendMessage("Dumped heap to " + file.getAbsolutePath()).queue();
                } catch (RuntimeException e) {
                    GabrielBot.LOGGER.error("Error dumping heap", e);
                    event.getChannel().sendMessage("Error dumping heap: " + e).queue();
                }
                break;
            case "threads":
                try {
                    DumpUtils.dumpThreads(args[1]);
                    event.getChannel().sendMessage("Dumped heap to " + file.getAbsolutePath()).queue();
                } catch (IOException e) {
                    GabrielBot.LOGGER.error("Error dumping heap", e);
                    event.getChannel().sendMessage("Error dumping heap: " + e).queue();
                }
                break;
            default:
                thiz.onHelp(event);
        }
    }

    @Command(
            name = "blacklist",
            description = "Adds or removes an user/guild from the blacklist",
            usage = "`>>blacklist add <id>`: Adds specified id to the blacklist\n" +
                    "`>>blacklist remove <id>`: Removes specified id from the blacklist",
            permission = CommandPermission.OWNER,
            category = CommandCategory.OWNER
    )
    public static void blacklist(@Argument("this") CommandReference thiz, @Argument("event")GuildMessageReceivedEvent event, @Argument("args") String[] args) {
        if(args.length < 2) {
            thiz.onHelp(event);
            return;
        }
        String id = args[1].replaceAll("(<@!?)?(\\d+?)(>)?", "$2");
        try {
            Long.parseLong(id);
        } catch(NumberFormatException e) {
            event.getChannel().sendMessage(id + ": Not a valid id").queue();
            return;
        }
        switch(args[0]) {
            case "add":
                GabrielData.blacklist().set(id, "true");
                event.getChannel().sendMessage("Added " + id + " to the blacklist").queue();
                return;
            case "remove":
                GabrielData.blacklist().remove(id);
                event.getChannel().sendMessage("Removed " + id + " from the blacklist").queue();
                return;
        }
        thiz.onHelp(event);
    }

    @Command(
            name = "lua",
            description = "Evaluates lua code",
            usage = "`>>lua <code>`",
            permission = CommandPermission.OWNER,
            category = CommandCategory.OWNER,
            advancedSplit = false
    )
    public static void lua(@Argument("this") CommandReference thiz, @Argument("event")GuildMessageReceivedEvent event, @Argument("args") String[] args) {
        if(args.length == 0) {
            thiz.onHelp(event);
            return;
        }
        String code = String.join(" ", args);
        Globals globals = JsePlatform.debugGlobals();
        globals.set("event", LuaHelper.coerce(event));
        Thread thread = new Thread(() -> {
            try {
                LuaValue v = globals.load(code).call();
                String s = v.tojstring();
                event.getChannel().sendMessage(v.isnil() ? "Executed successfully with no returns" : "Executed successfully and returned " + (s.length() > 500 ? Utils.paste(s) : String.format("```%n%s```", s))).queue();
            } catch(LuaError e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.close();
                String s = sw.toString();
                if (s.length() > 500) s = Utils.paste(s);
                event.getChannel().sendMessage("Error executing: ```\n" + s + "```").queue();
            }
        }, "LuaThread");
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    @Command(
            name = "eval",
            description = "Evaluates java code",
            usage = "`>>eval <code>`",
            permission = CommandPermission.OWNER,
            category = CommandCategory.OWNER,
            advancedSplit = false
    )
    public static void eval(@Argument("this") CommandReference thiz, @Argument("event")GuildMessageReceivedEvent event, @Argument("args") String[] args) {
        if(args.length == 0) {
            thiz.onHelp(event);
            return;
        }
        Thread thread = new Thread(() -> {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            String input = String.join(" ", args);
            String code = imports +
                    "public class Source {\n" +
                    "public static Object run(GuildMessageReceivedEvent event) throws Throwable {\n" +
                    "try {\n" +
                    "return null;\n" +
                    "} finally {\n" +
                    (input  + ";").replaceAll(";{2,}", ";") + "//*/\n" +
                    "}\n" +
                    "}\n" +
                    "}";
            File root = new File(".dynamic");
            File source = new File(root, "dontuse/Source.java");
            source.getParentFile().mkdirs();
            try {
                try(FileOutputStream fos = new FileOutputStream(source)) {
                    Utils.copyData(new ByteArrayInputStream(code.getBytes(Charset.defaultCharset())), fos);
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int errcode = compiler.run(null, out, out, source.getPath());
                if(errcode != 0) {
                    String err = new String(out.toByteArray(), Charset.defaultCharset());
                    if (err.length() > 500) {
                        err = Utils.paste(err);
                    }
                    event.getChannel().sendMessage("Error compiling:\n\n```\n" + err + "```").queue();
                    return;
                }
                DummyClassLoader loader = new DummyClassLoader(OwnerCommands.class.getClassLoader());
                File[] files = new File(root, "dontuse").listFiles();
                if(files == null) files = new File[0];
                for(File f : files) {
                    if(!f.getName().endsWith(".class")) continue;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try(FileInputStream fis = new FileInputStream(f)) {
                        Utils.copyData(fis, baos);
                    }
                    f.delete();
                    String clname = f.getAbsolutePath().substring(root.getAbsolutePath().length() + 1).replace('/', '.').replace('\\', '.');

                    UnsafeUtils.defineClass(
                            clname.substring(0, clname.length() - 6),
                            baos.toByteArray(),
                            loader
                    );
                }
                Object ret = loader.loadClass("dontuse.Source").getMethod("run", GuildMessageReceivedEvent.class).invoke(null, event);
                if(ret == null) {
                    event.getChannel().sendMessage("Evaluated successfully with no returns").queue();
                    return;
                }
                String v;
                if(ret instanceof Object[]) v = Arrays.toString((Object[]) ret);
                else if(ret instanceof boolean[]) v = Arrays.toString((boolean[]) ret);
                else if(ret instanceof byte[]) v = Arrays.toString((byte[]) ret);
                else if(ret instanceof short[]) v = Arrays.toString((short[]) ret);
                else if(ret instanceof char[]) v = new String((char[]) ret);
                else if(ret instanceof int[]) v = Arrays.toString((int[]) ret);
                else if(ret instanceof float[]) v = Arrays.toString((float[]) ret);
                else if(ret instanceof long[]) v = Arrays.toString((long[]) ret);
                else if(ret instanceof double[]) v = Arrays.toString((double[]) ret);
                else v = String.valueOf(ret);

                if(v.length() > 500) {
                    event.getChannel().sendMessage("Evaluated successfully: " + Utils.paste(v)).queue();
                    return;
                }
                event.getChannel().sendMessage("Evaluated successfully:\n\n```\n" + v + "```").queue();
            } catch(Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.close();
                String s = sw.toString();
                if (s.length() > 500) s = Utils.paste(s);
                event.getChannel().sendMessage("Error executing: ```\n" + s + "```").queue();
            }
        }, "EvalThread");
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    private static class DummyClassLoader extends ClassLoader {
        DummyClassLoader(ClassLoader parent) {
            super(parent);
        }
    }
}
