package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.core.command.*;
import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import br.net.brjdevs.natan.gabrielbot.utils.DumpUtils;
import br.net.brjdevs.natan.gabrielbot.utils.UnsafeUtils;
import br.net.brjdevs.natan.gabrielbot.utils.Utils;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;

@RegisterCommand.Class
public class OwnerCommands {
    private static String imports = "package dontuse;\n";

    static {
        addImport("java.util.*");
        addImport("java.net.*");
        addImport("java.math.*");
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
        addImport("br.net.brjdevs.natan.gabrielbot.core.command.custom.*");
        addImport("br.net.brjdevs.natan.gabrielbot.core.command.custom.functions.*");
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

    @RegisterCommand
    public static void save(CommandRegistry cr) {
        cr.register("save", SimpleCommand.builder(CommandCategory.OWNER)
                .description("save", "Flushes data to db")
                .help((thiz, event)->thiz.helpEmbed(event, "save", "`>>save`"))
                .code((event, args)->{
                    GabrielData.save();
                    event.getChannel().sendMessage("Successfully saved").queue();
                })
                .build()
        );
    }

    @RegisterCommand
    public static void shutdown(CommandRegistry cr) {
        cr.register("shutdown", SimpleCommand.builder(CommandCategory.OWNER)
                .description("shutdown", "Puts me to sleep")
                .help((thiz, event)->thiz.helpEmbed(event, "shutdown", "`>>shutdown`"))
                .code((event, args)->{
                    event.getChannel().sendMessage("*Goes to sleep...*").complete();
                    Arrays.stream(GabrielBot.getInstance().getShards()).forEach(s->s.getJDA().shutdown(true));
                    System.exit(0);
                })
                .build());
    }

    @RegisterCommand
    public static void dump(CommandRegistry cr) {
        cr.register("dump", SimpleCommand.builder(CommandCategory.OWNER)
                .description("dump", "Dumpb threads or heap to a file")
                .help((thiz, event)->thiz.helpEmbed(event, "dump",
                        "`>>dump heap <file>`: Dumps the heap to the given file\n" +
                                "`>>dump threads <file>`: Dumps the threads to the given file"
                ))
                .code((thiz, event, args)->{
                    if(args.length < 2) {
                        thiz.onHelp(event);
                        return;
                    }
                    File file = new File(args[1]);
                    //file.getParentFile().mkdirs();
                    switch(args[0]) {
                        case "heap":
                            try {
                                DumpUtils.dumpHeap(args[1]);
                                event.getChannel().sendMessage("Dumped heap to " + file.getAbsolutePath()).queue();
                            } catch(RuntimeException e) {
                                GabrielBot.LOGGER.error("Error dumping heap", e);
                                event.getChannel().sendMessage("Error dumping heap: " + e).queue();
                            }
                            break;
                        case "threads":
                            try {
                                DumpUtils.dumpThreads(args[1]);
                                event.getChannel().sendMessage("Dumped heap to " + file.getAbsolutePath()).queue();
                            } catch(IOException e) {
                                GabrielBot.LOGGER.error("Error dumping heap", e);
                                event.getChannel().sendMessage("Error dumping heap: " + e).queue();
                            }
                            break;
                        default:

                    }
                })
                .build());
    }

    @RegisterCommand
    public static void blacklist(CommandRegistry cr) {
        cr.register("blacklist", SimpleCommand.builder(CommandCategory.OWNER)
                .description("blacklist", "Adds or removes an user/guild from the blacklist")
                .help((thiz, event)->thiz.helpEmbed(event, "blacklist",
                        "`>>blacklist add <id>`: Adds specified id to the blacklist\n" +
                               "`>>blacklist remove <id>`: Removes specified id from the blacklist"
                ))
                .code((thiz, event, args)->{
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
                    event.getChannel().sendMessage(thiz.help(event)).queue();
                })
                .build());
    }

    @RegisterCommand
    public static void eval(CommandRegistry cr) {
        cr.register("eval", SimpleCommand.builder(CommandCategory.OWNER)
                .description("eval", "Evaluates code")
                .help((thiz, event)->thiz.helpEmbed(event, "eval",
                        "`>>eval <code>`: evaluates java code"
                ))
                .splitter(event->{
                    String[] s = event.getMessage().getRawContent().split(" ", 2);
                    return s.length == 1 ? new String[0] : s[1].split(" ");
                })
                .code((thiz, event, args)->{
                    if(args.length == 0) {
                        event.getChannel().sendMessage(thiz.help(event)).queue();
                        return;
                    }
                    Thread thread = new Thread(()->{
                        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                        String input = (String.join(" ", args) + ";").replaceAll(";{2,}", ";");
                        String code = imports +
                                "public class Source {\n" +
                                "public static Object run(GuildMessageReceivedEvent event) throws Throwable {\n" +
                                "try {\n" +
                                "return null;\n" +
                                "} finally {\n" +
                                input + "//*/\n" +
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
                                if(err.length() > 500) {
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
                                String clname = f.getAbsolutePath().substring(root.getAbsolutePath().length()+1).replace('/', '.').replace('\\', '.');

                                UnsafeUtils.defineClass(
                                        clname.substring(0, clname.length()-6),
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
                            if(ret instanceof Object[]) v = Arrays.toString((Object[])ret);
                            else if(ret instanceof boolean[]) v = Arrays.toString((boolean[])ret);
                            else if(ret instanceof byte[]) v = Arrays.toString((byte[])ret);
                            else if(ret instanceof short[]) v = Arrays.toString((short[])ret);
                            else if(ret instanceof char[]) v = new String((char[])ret);
                            else if(ret instanceof int[]) v = Arrays.toString((int[])ret);
                            else if(ret instanceof float[]) v = Arrays.toString((float[])ret);
                            else if(ret instanceof long[]) v = Arrays.toString((long[])ret);
                            else if(ret instanceof double[]) v = Arrays.toString((double[])ret);
                            else v = String.valueOf(ret);

                            if(v.length() > 500) {
                                event.getChannel().sendMessage("Evaluated successfully: " + Utils.paste(v)).queue();
                                return;
                            }
                            event.getChannel().sendMessage("Evaluated successfully:\n\n```\n" + v + "```").queue();
                        } catch(Exception e) {
                            e.printStackTrace();
                            event.getChannel().sendMessage("Error executing, check logs").queue();
                        }
                    }, "EvalThread");
                    thread.setPriority(Thread.MAX_PRIORITY);
                    thread.setDaemon(true);
                    thread.start();
                })
                .build());
    }

    private static class DummyClassLoader extends ClassLoader {
        DummyClassLoader(ClassLoader parent) {
            super(parent);
        }
    }
}
