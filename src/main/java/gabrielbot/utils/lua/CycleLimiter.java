package gabrielbot.utils.lua;

import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.DebugLib;

public class CycleLimiter extends DebugLib {
    private final int maxCycles;
    private int cycles = 0;

    CycleLimiter(int maxCycles) {
        this.maxCycles = maxCycles;
    }

    public int getCycles() {
        return cycles;
    }

    public int getMaxCycles() {
        return maxCycles;
    }

    public double getCyclesUsedPercentage() {
        return (double)cycles/(double)maxCycles;
    }

    @Override
    public void onInstruction(int pc, Varargs v, int top) {
        if (++cycles > maxCycles) throw new InstructionLimitException(maxCycles);
        super.onInstruction(pc, v, top);
    }
}
