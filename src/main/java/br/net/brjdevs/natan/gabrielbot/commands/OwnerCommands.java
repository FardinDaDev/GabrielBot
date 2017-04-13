package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandPermission;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandRegistry;
import br.net.brjdevs.natan.gabrielbot.core.command.RegisterCommand;
import br.net.brjdevs.natan.gabrielbot.core.command.SimpleCommand;
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
        addImport("br.net.brjdevs.natan.gabrielbot.core.data.*");
        addImport("br.net.brjdevs.natan.gabrielbot.core.jda.*");
        addImport("br.net.brjdevs.natan.gabrielbot.core.listeners.*");
        addImport("br.net.brjdevs.natan.gabrielbot.log.*");
        addImport("br.net.brjdevs.natan.gabrielbot.utils.*");
        addImport("br.net.brjdevs.natan.gabrielbot.utils.data.*");

        addImport("br.com.brjdevs.highhacks.eventbus.*");
    }

    public static void addImport(String name) {
        imports += "import " + name + ";\n";
    }

    @RegisterCommand
    public static void shutdown(CommandRegistry registry) {
        registry.register("shutdown", SimpleCommand.builder()
                .permission(CommandPermission.OWNER)
                .description("Puts me to sleep")
                .help(SimpleCommand.helpEmbed("shutdown", CommandPermission.OWNER, "Puts me to sleep", "`>>shutdown`"))
                .code((event, args)->{
                    event.getChannel().sendMessage("*Goes to sleep...*").complete();
                    Arrays.stream(GabrielBot.getInstance().getShards()).forEach(s->s.getJDA().shutdown(true));
                    System.exit(0);
                })
                .build());
    }

    @RegisterCommand
    public static void eval(CommandRegistry registry) {
        registry.register("eval", SimpleCommand.builder()
                .permission(CommandPermission.OWNER)
                .description("Evaluates code")
                .help(SimpleCommand.helpEmbed("eval", CommandPermission.OWNER,
                        "Evaluates code",
                        "`>>eval java <code>`: evaluates java code\n" +
                        "`>>eval js <code>`: evaluates javascript code"
                ))
                .code((event, args)->{
                    if(args.length < 2) {
                        event.getChannel().sendMessage(GabrielBot.getInstance().registry.commands().get("eval").help()).queue();
                        return;
                    }
                    switch(args[0]) {
                        case "java":
                            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                            String input = String.join(" ", Arrays.copyOfRange(args, 1, args.length)) + ";";
                            String code = imports +
                                    "public class Source {\n" +
                                        "public static Object run(GuildMessageReceivedEvent event){\n" +
                                            "try {\n" +
                                                "return null;\n" +
                                            "} finally {\n" +
                                                input.replaceAll(";{2,}", ";") + "//*/\n" +
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
                                    event.getChannel().sendMessage("Error compiling:\n\n" + err).queue();
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
                            return;
                        case "js":
                        case "javascript":
                            return;
                    }
                    event.getChannel().sendMessage(GabrielBot.getInstance().registry.commands().get("eval").help()).queue();
                })
                .build());
    }

    public static class DummyClassLoader extends ClassLoader {
        public DummyClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> define(String name, byte[] bytes) {
            return super.defineClass(name, bytes, 0, bytes.length);
        }
    }
}
