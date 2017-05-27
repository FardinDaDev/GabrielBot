package br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes;

public class AddImpl extends MathOperation {
	@Override
	double apply(double d1, double d2) {
		return d1+d2;
	}
}
