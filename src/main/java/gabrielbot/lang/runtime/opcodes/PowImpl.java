package gabrielbot.lang.runtime.opcodes;

public class PowImpl extends MathOperation {
	@Override
	double apply(double d1, double d2) {
		return Math.pow(d1, d2);
	}
}
