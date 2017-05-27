package br.net.brjdevs.natan.gabrielbot.commands.fun;

import net.dv8tion.jda.core.entities.Guild;

import java.nio.ByteBuffer;

public class BrainfuckInterpreter {
    public final int maxCycleCount;
    public final int memory;

    public BrainfuckInterpreter(int maxCycleCount, int memory) {
        this.maxCycleCount = maxCycleCount;
        this.memory = memory;
    }

    public String process(char[] code, String input, Guild guild) {
        ByteBuffer bytes = ByteBuffer.allocateDirect(memory);
        int data = 0;
        char[] inChars = input.toCharArray();
        int inChar = 0;
        StringBuilder output = new StringBuilder();
        int cycleCount = 0;
        try {
            for (int instruction = 0; instruction < code.length; ++instruction) {
                cycleCount++;
                if (cycleCount > maxCycleCount) {
                    throw new BrainfuckException("Exceeded max amount of cycles (" + maxCycleCount + ")");
                }
                char command = code[instruction];
                switch (command) {
                    case '>':
                        ++data;
                        break;
                    case '<':
                        --data;
                        if(data < 0){
                            throw new BrainfuckException("Data pointer out of bounds");
                        }
                        break;
                    case '+':
                        bytes.put(data, (byte) (bytes.get(data) + 1));
                        break;
                    case '-':
                        bytes.put(data, (byte) (bytes.get(data) - 1));
                        break;
                    case '.':
                        output.append((char) bytes.get(data));
                        break;
                    case ',':
                        try {
                            bytes.put(data, (byte) inChars[inChar++]);
                            break;
                        } catch (IndexOutOfBoundsException ex) {
                            throw new BrainfuckException("Input out of bounds");
                        }
                    case '[':
                        if (bytes.get(data) == 0) {
                            int depth = 1;
                            do {
                                command = code[++instruction];
                                if (command == '[') {
                                    ++depth;
                                } else if (command == ']') {
                                    --depth;
                                }
                            } while (depth > 0);
                        }
                        break;
                    case ']':
                        if (bytes.get(data) != 0) {
                            int depth = -1;
                            do {
                                command = code[--instruction];
                                if (command == '[') {
                                    ++depth;
                                } else if (command == ']') {
                                    --depth;
                                }
                            } while (depth < 0);
                        }
                        break;
                }
            }
        } catch(IndexOutOfBoundsException e) {
            throw new BrainfuckException("Data pointer out of bounds");
        }
        return output.toString();
    }

    public static class BrainfuckException extends RuntimeException {
        public BrainfuckException(String s) {
            super(s, null, false, false);
        }

        public BrainfuckException(String s, Throwable t) {
            super(s, t, false, false);
        }
    }
}
