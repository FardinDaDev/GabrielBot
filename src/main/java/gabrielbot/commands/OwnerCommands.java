package gabrielbot.commands;

import gabrielbot.GabrielBot;
import gabrielbot.core.command.Argument;
import gabrielbot.core.command.Command;
import gabrielbot.core.command.CommandCategory;
import gabrielbot.core.command.CommandPermission;
import gabrielbot.core.command.CommandReference;
import gabrielbot.core.data.GabrielData;
import gabrielbot.core.jda.EventManagerThread;
import gabrielbot.core.listeners.StarboardListener;
import gabrielbot.core.listeners.operations.ReactionOperations;
import gabrielbot.music.SerializedPlayer;
import gabrielbot.music.SerializedTrack;
import gabrielbot.music.Track;
import gabrielbot.utils.DumpUtils;
import gabrielbot.utils.KryoUtils;
import gabrielbot.utils.UnsafeUtils;
import gabrielbot.utils.Utils;
import gabrielbot.utils.data.JedisDataManager;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
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

        addImport("gabrielbot.*");
        addImport("gabrielbot.commands.*");
        addImport("gabrielbot.commands.custom.*");
        addImport("gabrielbot.commands.game.*");
        addImport("gabrielbot.core.command.*");
        addImport("gabrielbot.core.data.*");
        addImport("gabrielbot.core.jda.*");
        addImport("gabrielbot.core.listeners.*");
        addImport("gabrielbot.core.listeners.operations.*");
        addImport("gabrielbot.log.*");
        addImport("gabrielbot.music.*");
        addImport("gabrielbot.utils.*");
        addImport("gabrielbot.utils.brainfuck.*");
        addImport("gabrielbot.utils.cache.*");
        addImport("gabrielbot.utils.commands.*");
        addImport("gabrielbot.utils.data.*");
        addImport("gabrielbot.utils.http.*");
        addImport("gabrielbot.utils.lua.*");
        addImport("gabrielbot.utils.pokeapi.*");
        addImport("gabrielbot.utils.starboard.*");
        addImport("gabrielbot.utils.stats.*");

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
    public static void save(@Argument("channel") TextChannel channel) {
        GabrielData.save();
        channel.sendMessage("Successfully saved").queue();
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
    public static void shutdown(@Argument("channel") TextChannel channel, @Argument("args") String[] args) {
        if (args.length > 0 && args[0].equals("savemusic")) {
            GabrielBot.getInstance().registry.commands().remove("play");
            JedisDataManager jdm = GabrielData.guilds();
            int[] i = new int[1];
            GabrielBot.getInstance().getPlayers().forEachValue(p->{
                if(p == null) return true;
                if(p.scheduler.currentTrack() == null) {
                    p.leave();
                    return true;
                }
                p.player.setPaused(true);
                List<Track> allTracks = new ArrayList<>();
                allTracks.add(p.scheduler.currentTrack());
                allTracks.addAll(p.scheduler.tracks());
                p.leave();
                p.textChannel.sendMessage("I'll be rebooting soon, but your queue has been saved and will be restored after I reboot").queue();
                SerializedPlayer sp = new SerializedPlayer(allTracks.stream().map(SerializedTrack::new).collect(Collectors.toList()), allTracks.get(0).track.getPosition(), p.guildId, p.textChannel.getIdLong(), p.voiceChannel.getIdLong());
                jdm.set("p_" + p.guildId, Base64.getEncoder().encodeToString(KryoUtils.serialize(sp)));
                i[0]++;
                return true;
            });
            jdm.save();
            GabrielBot.LOGGER.info("Serialized tracks for {} guilds", i[0]);
        }
        GabrielData.save();
        channel.sendMessage("*Goes to sleep...*").complete();
        Arrays.stream(GabrielBot.getInstance().getShards()).forEach(s -> s.getJDA().shutdown(true));
        System.exit(0);
    }

    @Command(
            name = "forcestar",
            description = "Forces addition or removal of stars in a starboard message",
            usage = "`>>forcestar add <id> <amount>`\n" +
                    "`>>forcestar remove <id> <amount>`",
            permission = CommandPermission.OWNER,
            category = CommandCategory.OWNER
    )
    public static void forcestar(@Argument("author") User user, @Argument("channel") TextChannel channel, @Argument("args") String[] args) {
        GabrielBot.getInstance().log("Running forcestar with " + Arrays.toString(args));
        long messageId = Long.parseLong(args[1]);
        ReactionOperations.OperationFuture future = ReactionOperations.get(messageId);
        StarboardListener.StarOperation operation = future == null ? null : (StarboardListener.StarOperation)future.operation;
        switch(args[0]) {
            case "add":
                operation.reactions.addAndGet(Integer.parseInt(args[2]));
                operation.update();
                break;
            case "remove":
                operation.reactions.addAndGet(-Integer.parseInt(args[2]));
                operation.update();
                break;
            default: throw new IllegalArgumentException("Unknown option " + args[0]);
        }
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
    @SuppressWarnings("all") //idk what's the "Result of x is unused" one and it was the only warning anyway
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
            name = "eval",
            description = "Evaluates java code",
            usage = "`>>eval <code>`",
            permission = CommandPermission.OWNER,
            category = CommandCategory.OWNER,
            advancedSplit = false
    )
    @SuppressWarnings("all") //same as blacklist
    public static void eval(@Argument("this") CommandReference thiz, @Argument("event")GuildMessageReceivedEvent event, @Argument("args") String[] args) {
        if(args.length == 0) {
            thiz.onHelp(event);
            return;
        }
        Thread thread = EventManagerThread.current().newThread(() -> {
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
            } catch(InvocationTargetException e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.getCause().printStackTrace(pw);
                pw.close();
                String s = sw.toString();
                if (s.length() > 500) {
                    event.getChannel().sendMessage("Error executing: " + Utils.paste(s)).queue();
                } else {
                    event.getChannel().sendMessage("Error executing: ```\n" + s + "```").queue();
                }
            } catch(Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.close();
                String s = sw.toString();
                if (s.length() > 500) {
                    event.getChannel().sendMessage("Error executing: " + Utils.paste(s)).queue();
                } else {
                    event.getChannel().sendMessage("Error executing: ```\n" + s + "```").queue();
                }
            }
        }, "EvalThread");
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private static class DummyClassLoader extends ClassLoader {
        DummyClassLoader(ClassLoader parent) {
            super(parent);
        }
    }
}
