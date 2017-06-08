package br.net.brjdevs.natan.gabrielbot.utils.lua;

import br.net.brjdevs.natan.gabrielbot.utils.brainfuck.BrainfuckInterpreter;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.Region;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LuaHelper {
    private static final String INSTANCE_KEY = "instance";

    private static final LuaFunction __eq = new LibFunction() {
        @Override
        public LuaValue call(LuaValue first, LuaValue second) {
            if(!(first.istable() && second.istable())) return FALSE;
            LuaValue one = getInstance(first), two = getInstance(second);
            if(!(one.isuserdata(Instance.class) && two.isuserdata(Instance.class))) return FALSE;
            return valueOf(((Instance)CoerceLuaToJava.coerce(one, Instance.class)).instance.equals(((Instance)CoerceLuaToJava.coerce(two, Instance.class)).instance));
        }
    };

    public static Globals setup(int maxCycles) {
        return setup(null, maxCycles, 0, null, null);
    }

    public static Globals setup(int maxCycles, String input) {
        return setup(null, maxCycles, 0, input, null);
    }

    public static Globals setup(GuildMessageReceivedEvent event, int maxCycles, int maxMessages, String input) {
        return setup(event, maxCycles, maxMessages, input, null);
    }

    public static Globals setup(GuildMessageReceivedEvent event, int maxCycles, int maxMessages, String input, StringBuilder out) {
        Globals globals = new Globals();
        globals.load(new PackageLib());
        globals.load(new JseBaseLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        globals.load(new JseMathLib());
        LoadState.install(globals);
        LuaC.install(globals);
        CycleLimiter limiter = new CycleLimiter(maxCycles);
        globals.load(limiter);
        globals.set("package", LuaValue.NIL);
        globals.set("debug", LuaValue.NIL);
        globals.set("dofile", LuaValue.NIL);
        globals.set("loadfile", LuaValue.NIL);
        globals.set("require", LuaValue.NIL);
        if(out == null) {
            globals.set("print", new LibFunction() {});//noop
        } else {
            globals.set("print", new VarArgFunction() {
                @Override
                public Varargs onInvoke(Varargs args) {
                    for(int i = 1, n=args.narg(); i <= n; i++) {
                        if (i > 1) out.append('\t');
                        out.append(args.arg(i).tojstring());
                    }
                    out.append('\n');
                    return NONE;
                }
            });
        }

        globals.set("brainfuck", new LibFunction() {
            @Override
            public Varargs invoke(Varargs v) {
                String code = v.arg1().checkstring().tojstring();
                LuaValue i = v.arg(2);
                String input = i.isnil() ? "" : i.checkstring().tojstring();
                BrainfuckInterpreter bf = new BrainfuckInterpreter(1000, 1<<10);
                try {
                    return valueOf(bf.process(code.toCharArray(), input));
                } catch(BrainfuckInterpreter.BrainfuckException e) {
                    return varargsOf(new LuaValue[]{NIL, valueOf(e.getMessage())});
                }
            }
        });

        globals.set("input", input);
        LuaValue[] args = Arrays.stream(input.split(" ")).map(LuaValue::valueOf).toArray(LuaValue[]::new);
        if(args.length != 0) globals.set("args", new LuaTable(null, args, null));
        if(event != null) {
            SafeGuildMessageReceivedEvent e = new SafeGuildMessageReceivedEvent(event, maxMessages);
            globals.set("user", coerce(e.getAuthor()));
            globals.set("channel", coerce(e.getChannel()));
            globals.set("member", coerce(e.getMember()));
            globals.set("guild", coerce(e.getGuild()));
            globals.set("message", coerce(e.getMessage()));
            globals.set("event", coerce(e));
        }

        return globals;
    }

    public static CycleLimiter getLimiter(Globals globals) {
        DebugLib l = globals.debuglib;
        if(l == null) return null;
        if(l instanceof CycleLimiter) {
            return (CycleLimiter)l;
        }
        throw new IllegalArgumentException("Provided globals were not created using setup()");
    }

    private static LuaValue getInstance(LuaValue t) {
        return t.getmetatable().istable() ? t.getmetatable().get(INSTANCE_KEY) : LuaValue.NIL;
    }

    public static LuaTable coerce(Object obj) {
        LuaTable object = new LuaTable();
        Class<?> cls = obj.getClass();
        for(Method m : cls.getMethods()) {
            Class<?> declaring = m.getDeclaringClass();
            if(declaring == Object.class) {
                switch(m.getName()) {
                    //not a good idea to let these stay
                    case "notify":
                    case "notifyAll":
                    case "wait":
                        continue;
                }
            } else if(declaring == Class.class) {
                switch(m.getName()) {
                    case "forName":
                    case "newInstance":
                    case "getResource":
                    case "getResourceAsStream":
                        continue;
                }
            }
            //no reflection
            Class<?> ret = m.getReturnType();
            while(ret.isArray()) ret = ret.getComponentType();
            if(ret.getName().startsWith("java.lang.reflect.") || ret.getName().startsWith("java.lang.annotation.") || ret == ClassLoader.class) continue;


            object.set(m.getName(), coerce(obj, m));
        }

        LuaTable ret = new LuaTable();
        LuaTable retMt = new LuaTable();
        Instance i = new Instance(obj);
        retMt.set(INSTANCE_KEY, CoerceJavaToLua.coerce(i));
        retMt.set("__index", object);
        retMt.set("__metatable", LuaValue.FALSE);
        retMt.set("__newindex", new LibFunction() {
            @Override
            public LuaValue call() {
                return error("Objects cannot be modified");
            }
        });
        retMt.set("__tostring", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return valueOf(obj.toString());
            }
        });
        retMt.set("__eq", __eq);
        retMt.set("__call", new LibFunction() {
            @Override
            public LuaValue call() {
                StringBuilder sb = new StringBuilder();
                for(LuaValue key : object.keys()) {
                    sb.append(key.tojstring()).append(", ");
                }
                if(sb.length() != 0) sb.deleteCharAt(sb.length()-2);
                return valueOf("Available methods: [" + sb + "]");
            }
        });

        ret.setmetatable(retMt);
        return ret;
    }

    public static LuaFunction coerce(Object instance, Method method) {
        Class<?>[] params = method.getParameterTypes();
        return new VarArgFunction() {
            @Override
            public Varargs invoke(LuaValue[] values) {
                Object[] args = new Object[params.length];
                for(int i = 0; i < Math.min(params.length, values.length); i++) {
                    LuaValue v = values[i];
                    if(v.istable()) {
                        LuaValue l = getInstance(v);
                        if(l.isuserdata(Instance.class)) {
                            Object o = ((Instance)CoerceLuaToJava.coerce(l, Instance.class)).instance;
                            if(o != null && !params[i].isInstance(o)) {
                                return varargsOf(new LuaValue[]{NIL, valueOf("TypeError: expected " + params[i].getName() + ", got " + o.getClass().getName())});
                            }
                            args[i] = o;
                            continue;
                        }
                    }
                    args[i] = CoerceLuaToJava.coerce(v, params[i]);
                }
                try {
                    Object o = method.invoke(instance, args);
                    if(o == null) return NIL;
                    LuaValue v = CoerceJavaToLua.coerce(o);
                    if(v.isuserdata()) {
                        return coerce(o);
                    }
                    return v;
                } catch(InvocationTargetException e) {
                    throw new LuaError(e.getCause());
                } catch(Exception e) {
                    throw new LuaError(e);
                }
            }

            @Override
            public Varargs onInvoke(Varargs args) {
                LuaValue[] v = new LuaValue[args.narg()];
                for(int i = 0; i < v.length;) {
                    v[i] = args.arg(++i);
                }
                return invoke(v);
            }
        };
    }

    private static class SafeISnowflake {
        private final ISnowflake snowflake;

        SafeISnowflake(ISnowflake snowflake) {
            this.snowflake = snowflake;
        }

        public String getId() {
            return snowflake.getId();
        }

        public long getIdLong() {
            return snowflake.getIdLong();
        }

        public OffsetDateTime getCreationTime() {
            return snowflake.getCreationTime();
        }
    }

    private static class SafeGuildMessageReceivedEvent {
        private final SafeChannel channel;
        private final SafeUser author;
        private final SafeMember member;
        private final SafeGuild guild;
        private final SafeMessage message;

        SafeGuildMessageReceivedEvent(GuildMessageReceivedEvent event, int maxMessages) {
            this.channel = new SafeChannel(event.getChannel(), maxMessages);
            this.author = new SafeUser(event.getAuthor());
            this.member = new SafeMember(event.getMember());
            this.guild = new SafeGuild(event.getGuild(), channel);
            this.message = new SafeMessage(event.getMessage());
        }

        public SafeChannel getChannel() {
            return channel;
        }

        public SafeUser getAuthor() {
            return author;
        }

        public SafeMember getMember() {
            return member;
        }

        public SafeGuild getGuild() {
            return guild;
        }

        public SafeMessage getMessage() {
            return message;
        }
    }

    private static class SafeUser extends SafeISnowflake {
        private final User user;

        SafeUser(User user) {
            super(user);
            this.user = user;
        }

        public String getName() {
            return user.getName();
        }

        public String getDiscriminator() {
            return user.getDiscriminator();
        }

        public String getAvatarUrl() {
            return user.getEffectiveAvatarUrl();
        }

        public boolean isBot() {
            return user.isBot();
        }

        public String getAsMention() {
            return user.getAsMention();
        }
    }

    private static class SafeChannel extends SafeISnowflake {
        private final MessageChannel channel;
        private final int maxMessages;
        private int messages = 0;

        SafeChannel(MessageChannel channel, int maxMessages) {
            super(channel);
            this.channel = channel;
            this.maxMessages = maxMessages;
        }

        public void sendMessage(String message) {
            if(++messages >= maxMessages) throw new LuaError("Maximum amount of messages reached");
            channel.sendMessage(message).queue();
        }
    }

    private static class SafeMessage extends SafeISnowflake {
        private final Message message;

        SafeMessage(Message message) {
            super(message);
            this.message = message;
        }

        public String getContent() {
            return message.getContent();
        }

        public String getRawContent() {
            return message.getRawContent();
        }

        public String getStrippedContent() {
            return message.getStrippedContent();
        }
    }

    private static class SafeGuild extends SafeISnowflake {
        private final Guild guild;
        private final SafeChannel channel;

        SafeGuild(Guild guild, SafeChannel channel) {
            super(guild);
            this.guild = guild;
            this.channel = channel;
        }

        public String getName() {
            return guild.getName();
        }

        public Guild.ExplicitContentLevel getExplicitContentLevel() {
            return guild.getExplicitContentLevel();
        }

        public Region getRegion() {
            return guild.getRegion();
        }

        public List<SafeChannel> getTextChannels() {
            return guild.getTextChannels().stream().map(c->c.getIdLong() == channel.getIdLong() ? channel : new SafeChannel(c, 0)).collect(Collectors.toList());
        }

        public List<SafeRole> getRoles() {
            return guild.getRoles().stream().map(SafeRole::new).collect(Collectors.toList());
        }

        public List<SafeMember> getMembers() {
            return guild.getMembers().stream().map(SafeMember::new).collect(Collectors.toList());
        }

        public SafeMember getOwner() {
            return new SafeMember(guild.getOwner());
        }

        public String getIconUrl() {
            return guild.getIconUrl();
        }
    }

    private static class SafeMember {
        private final Member member;

        SafeMember(Member member) {
            this.member = member;
        }

        public Color getColor() {
            return member.getColor();
        }

        public OffsetDateTime getJoinDate() {
            return member.getJoinDate();
        }

        public Game getGame() {
            return member.getGame();
        }

        public OnlineStatus getOnlineStatus() {
            return member.getOnlineStatus();
        }

        public String getName() {
            return member.getEffectiveName();
        }

        public boolean isOwner() {
            return member.isOwner();
        }

        public List<SafeRole> getRoles() {
            return member.getRoles().stream().map(SafeRole::new).collect(Collectors.toList());
        }
    }

    private static class SafeRole extends SafeISnowflake {
        private final Role role;

        SafeRole(Role role) {
            super(role);
            this.role = role;
        }

        public String getName() {
            return role.getName();
        }

        public List<Permission> getPermissions() {
            return role.getPermissions();
        }

        public long getPermissionsRaw() {
            return role.getPermissionsRaw();
        }

        public int getPosition() {
            return role.getPosition();
        }

        public int getPositionRaw() {
            return role.getPositionRaw();
        }

        public boolean isManaged() {
            return role.isManaged();
        }

        public boolean isPublicRole() {
            return role.isPublicRole();
        }

        public boolean isMentionable() {
            return role.isMentionable();
        }

        public boolean isSeparate() {
            return role.isHoisted();
        }
    }

    private static class Instance {
        final Object instance;

        Instance(Object instance) {
            this.instance = instance;
        }
    }
}
