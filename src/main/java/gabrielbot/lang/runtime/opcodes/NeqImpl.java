package gabrielbot.lang.runtime.opcodes;

public class NeqImpl extends NumberComparator {
	@Override
	boolean apply(double d1, double d2) {
		return d1 != d2;
	}
}
