package gabrielbot.lang.runtime.opcodes;

public class MulImpl extends MathOperation {
	@Override
	double apply(double d1, double d2) {
		return d1*d2;
	}
}
