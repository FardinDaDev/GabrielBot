package br.net.brjdevs.natan.gabrielbot.commands.custom;

import br.net.brjdevs.natan.gabrielbot.utils.lua.InstructionLimitException;
import br.net.brjdevs.natan.gabrielbot.utils.lua.LuaHelper;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.luaj.vm2.LuaError;
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
            LuaValue ret = LuaHelper.setup(event, 10000, 0, input, sb).load(code).call();
            return sb.toString().trim().length() == 0 ? ret.tojstring() : sb.toString();
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
