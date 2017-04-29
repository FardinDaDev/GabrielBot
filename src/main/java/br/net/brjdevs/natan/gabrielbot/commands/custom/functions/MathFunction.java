package br.net.brjdevs.natan.gabrielbot.commands.custom.functions;

public class MathFunction extends JavaFunction {
    private static final ArithmeticException INVALID_OP = new ArithmeticException();

    @Override
    public String apply(String[] args) {
        if(args.length < 3 || !(isNumber(args[0]) && isNumber(args[2]))) return "invalid usage";
        double val;
        try {
            val = calculate(Double.parseDouble(args[0]), args[1], Double.parseDouble(args[2]));
        } catch(ArithmeticException ex) {
            return "invalid operation";
        }
        return val % 1 == 0 ? String.valueOf((long)val) : String.valueOf(val);
    }

    private static double calculate(double first, String op, double second) {
        switch(op) {
            case "+":
                return first+second;
            case "-":
                return first-second;
            case "*":
                return first*second;
            case "/":
            case ":":
            case "÷":
                return first/second;
            case "%":
                return first%second;
            case "^":
                return Math.pow(first, second);
            case ">>":
                return (long)first>>(long)second;
            case "<<":
                return (long)first<<(long)second;
            case ">>>":
                return (long)first>>>(long)second;
            default:
                throw INVALID_OP;
        }
    }
}
