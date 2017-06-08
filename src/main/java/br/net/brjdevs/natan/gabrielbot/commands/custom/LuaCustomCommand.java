package br.net.brjdevs.natan.gabrielbot.commands.custom;

import br.net.brjdevs.natan.gabrielbot.utils.lua.InstructionLimitException;
import br.net.brjdevs.natan.gabrielbot.utils.lua.LuaHelper;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;

import java.util.Map;

public class LuaCustomCommand extends CustomCommand {
    private final String code;

    public LuaCustomCommand(String code) {
        this.code = code;
    }

    @Override
    public String process(GuildMessageReceivedEvent event, String input, Map<String, String> mappings) {
        try {
            StringBuilder sb = new StringBuilder();
            Globals g = LuaHelper.setup(event, 10000, 0, input, sb);
            LuaFunction tostring = g.get("tostring").checkfunction();
            LuaValue ret = g.load(code).call();
            String s = sb.toString();
            return ret.isnil() ? s.trim().isEmpty() ? null : s : tostring.call(ret).tojstring();
        } catch (LuaError error) {
            Throwable cause = error.getCause();
            if (cause instanceof InstructionLimitException) {
                return "Cycle limit exceeded";
            }
            if (cause != null) return "Error executing: " + cause;
            return error.getMessage();
        }
    }

    @Override
    public String getRaw() {
        return code;
    }
}
