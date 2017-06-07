package br.net.brjdevs.natan.gabrielbot.utils.lua;

public class InstructionLimitException extends RuntimeException {
    private final int maxCycles;

    InstructionLimitException(int maxCycles) {
        this.maxCycles = maxCycles;
    }

    public int getMaxCycles() {
        return maxCycles;
    }
}
