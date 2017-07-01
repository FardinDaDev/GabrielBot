package gabrielbot.utils;

@FunctionalInterface
public interface IntIntObjectFunction<T> {
    T apply(int i, int j);
}
