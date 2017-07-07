package gabrielbot.commands.custom;

import gabrielbot.GabrielBot;
import gabrielbot.lang.common.Opcodes;
import gabrielbot.lang.compiler.Parser;
import gabrielbot.lang.runtime.Array;
import gabrielbot.lang.runtime.Interpreter;
import gabrielbot.lang.runtime.Verifier;
import gabrielbot.lang.runtime.invoke.Method;
import gabrielbot.lang.runtime.opcodes.InvokeStaticImpl;
import gabrielbot.utils.Randoms;
import gabrielbot.utils.StringUtils;
import gabrielbot.utils.UnsafeUtils;
import gabrielbot.utils.brainfuck.BrainfuckInterpreter;
import gabrielbot.utils.commands.Jokes;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CodeCustomCommand extends CustomCommand {
    public static int debugFlags = GabrielBot.DEBUG ? 0xFFFFFFFF : 0;

    static {
        Method.register("brainfuck", new Method() {
            @Override
            public Object run(Interpreter interpreter, Object instance, Object... args) {
                return new BrainfuckInterpreter(5000, 1 << 12).process(
                        String.valueOf(args[0]).toCharArray(),
                        String.valueOf(args[1])
                );
            }

            @Override
            public int args() {
                return 2;
            }

            @Override
            public boolean isStatic() {
                return true;
            }
        });
        Method.register("joke", new Method() {
            @Override
            public Object run(Interpreter interpreter, Object instance, Object... args) {
                return Jokes.getJoke(null);
            }

            @Override
            public int args() {
                return 0;
            }

            @Override
            public boolean isStatic() {
                return true;
            }
        });
        Method.register("join", new Method() {
            @Override
            public Object run(Interpreter interpreter, Object instance, Object... args) {
                return ((Array) args[0]).stream().map(String::valueOf).collect(Collectors.joining(" "));
            }

            @Override
            public int args() {
                return 1;
            }

            @Override
            public boolean isStatic() {
                return true;
            }
        });
        Method.register("url", new Method() {
            @Override
            public Object run(Interpreter interpreter, Object instance, Object... args) {
                try {
                    return URLEncoder.encode(String.valueOf(args[0]), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    UnsafeUtils.throwException(e);
                    throw new AssertionError();
                }
            }

            @Override
            public int args() {
                return 1;
            }

            @Override
            public boolean isStatic() {
                return true;
            }
        });
        Method.register("replace", new Method() {
            @Override
            public Object run(Interpreter interpreter, Object instance, Object... args) {
                return String.valueOf(args[0]).replace(String.valueOf(args[1]), String.valueOf(args[2]));
            }

            @Override
            public int args() {
                return 3;
            }

            @Override
            public boolean isStatic() {
                return true;
            }
        });
        Method.register("range", new Method() {
            @Override
            public Object run(Interpreter interpreter, Object instance, Object... args) {
                int i1 = ((Number) args[0]).intValue();
                int i2 = ((Number) args[1]).intValue();
                if (i2 > i1) {
                    i1 = i1 ^ i2;
                    i2 = i1 ^ i2;
                    i1 = i1 ^ i2;
                }
                return Randoms.nextInt(i1) + i2;
            }

            @Override
            public int args() {
                return 2;
            }

            @Override
            public boolean isStatic() {
                return true;
            }
        });
        Method.register("random", new Method() {
            @Override
            public Object run(Interpreter interpreter, Object instance, Object... args) {
                Array a = (Array) args[0];
                return a.getAtIndex(Randoms.nextInt(a.size())); //gets an existing index for sparse arrays
            }

            @Override
            public int args() {
                return 1;
            }

            @Override
            public boolean isStatic() {
                return true;
            }
        });
    }

    private final byte[] bytecode;

    public CodeCustomCommand(byte[] bytecode) {
        this.bytecode = bytecode;
    }

    @Override
    public String process(GuildMessageReceivedEvent event, String input, Map<String, String> mappings) {
        Interpreter interpreter = new Interpreter(bytecode, StringUtils.advancedSplitArgs(input, 0), debugFlags);
        interpreter.globals.putAll(mappings);
        StringBuilder sb = new StringBuilder();
        ((InvokeStaticImpl) interpreter.getOpcodeImplementationByOpcode(Opcodes.INVOKESTATIC)).register("println", new Method() {
            @Override
            public Object run(Interpreter interpreter, Object instance, Object... args) {
                sb.append(args[0]).append('\n');
                return VOID;
            }

            @Override
            public int args() {
                return 1;
            }

            @Override
            public boolean isStatic() {
                return true;
            }
        });
        try {
            interpreter.run();
        } catch (Throwable t) {
            return "Error processing: " + t;
        }
        return sb.toString();
    }

    @Override
    public String getRaw() {
        return "Raw data not supported for code commands yet";
    }

    public static CodeCustomCommand of(GuildMessageReceivedEvent event, String text) {
        text =
                extern("println", 1) +
                        extern("brainfuck", 2) +
                        extern("joke", 0) +
                        extern("join", 1) +
                        extern("url", 1) +
                        extern("replace", 3) +
                        extern("range", 2) +
                        extern("random", 1) +
                        text;
        try {
            byte[] bytes = Parser.parse(text);
            Verifier.verify(bytes);
            return new CodeCustomCommand(bytes);
        } catch (Throwable t) {
            event.getChannel().sendMessage("Error compiling: " + t.getMessage()).queue();
            return null;
        }
    }

    private static String extern(String name, int args) {
        return "extern func " + name + "(" + IntStream.range(0, args).mapToObj(i -> "arg" + i).collect(Collectors.joining(",")) + "); ";
    }
}
