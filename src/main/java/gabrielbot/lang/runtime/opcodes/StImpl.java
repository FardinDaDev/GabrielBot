package gabrielbot.lang.runtime.opcodes;

public class StImpl extends NumberComparator {
	@Override
	boolean apply(double d1, double d2) {
		return d1 < d2;
	}
}
