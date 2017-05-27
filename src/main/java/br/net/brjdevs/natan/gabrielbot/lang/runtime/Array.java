package br.net.brjdevs.natan.gabrielbot.lang.runtime;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Array {
	private Entry[] entries;
	private int size;
	
	public Array(int initialSize) {
		entries = new Entry[initialSize];
	}
	
	public Array() {
		this(10);
	}
	
	private void resize() {
		Entry[] n = new Entry[(int)(entries.length * 1.5)];
		System.arraycopy(entries, 0, n, 0, entries.length);
		entries = n;
	}
	
	public Object get(int i) {
		for(Entry e : entries) {
			if(e != null && e.key == i) {
				return e.value;
			}
		}
		return null;
	}
	
	public Object set(int i, Object o) {
		for(Entry e : entries) {
			if(e != null && e.key == i) {
				Object r = e.value;
				e.value = o;
				return r;
			}
		}
		Entry e = new Entry();
		e.key = i;
		e.value = o;
		if(entries.length < size + 1) {
			resize();
		}
		entries[size++] = e;
		return null;
	}

	public int size() {
	    return size;
    }

    public Object getAtIndex(int index) {
	    if(index < 0 || index > size) throw new ArrayIndexOutOfBoundsException(index);
	    return entries[index].value;
    }
	
	public static Array fromArray(Object array) {
		Array a = new Array();
		if(!array.getClass().isArray()) {
			throw new IllegalArgumentException(array + ": not an array");
		}
		int i = 0;
		if(array instanceof Object[]) {
			for(Object o : (Object[])array) {
				a.set(i++, o);
			}
		} else if(array instanceof int[]) {
			for(int n : (int[])array) {
				a.set(i++, n);
			}
		} else if(array instanceof long[]) {
			for(long l : (long[])array) {
				a.set(i++, l);
			}
		} else if(array instanceof char[]) {
			for(char c : (char[])array) {
				a.set(i++, c);
			}
		} else if(array instanceof byte[]) {
			for(byte b : (byte[])array) {
				a.set(i++, b);
			}
		} else if(array instanceof boolean[]) {
			for(boolean b : (boolean[])array) {
				a.set(i++, b);
			}
		} else if(array instanceof float[]) {
			for(float f : (float[])array) {
				a.set(i++, f);
			}
		} else if(array instanceof double[]) {
			for(double d : (double[])array) {
				a.set(i++, d);
			}
		} else if(array instanceof short[]) {
			for(short s : (short[])array) {
				a.set(i++, s);
			}
		} else {
			throw new AssertionError(array.getClass());
		}
		return a;
	}

	public Stream<Object> stream() {
	    return Arrays.stream(entries).filter(Objects::nonNull).map(e->e.value);
    }

    public void forEach(Consumer<Object> consumer) {
	    for(Entry e : entries) {
	        if(e == null) return;
	        consumer.accept(e.value);
        }
    }
	
	@Override
	public String toString() {
		return Arrays.stream(entries).filter(Objects::nonNull).map(e->{
		    if(e.value == Array.this) return "[" + e.key + "] = This array";
		    return "[" + e.key + "] = " + String.valueOf(e.value);
		}).collect(Collectors.joining(", ", "[", "]"));
	}
	
	@Override
	public boolean equals(Object other) {
		if(other instanceof Array) {
			Array o = (Array)other;
			if(o.size != size) return false;
			for(Entry e : o.entries) {
				if(e == null) return true;
				if(!Objects.equals(e.value, get(e.key))) return false;
			}
		}
		return false;
	}

    private static class Entry {
        Object value;
        int key;
    }
}
