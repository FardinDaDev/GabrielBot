package br.net.brjdevs.natan.gabrielbot.core.data.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class VersionSerializer<T> extends Serializer<T> {
    private final Version version;
    private final Map<Field, Since> fields;

    public VersionSerializer(Class<T> clazz) {
        version = clazz.getAnnotation(Version.class);
        if(version == null) throw new IllegalArgumentException("Class not annotated with @Version: " + clazz.getName());
        Map<Field, Since> map = new HashMap<>();
        Class<?> c = clazz;
        while(c != Object.class) {
            for(Field f : c.getDeclaredFields()) {
                if(Modifier.isTransient(f.getModifiers())) continue;
                Since s = f.getAnnotation(Since.class);
                if(s == null) throw new IllegalArgumentException("Field not annotated with @Since: " + f);
                if(s.value() > version.value()) throw new IllegalArgumentException("@Since version greater than @Version: " + f);
                map.put(f, s);
            }
            c = c.getSuperclass();
        }
        AccessibleObject.setAccessible(map.keySet().toArray(new AccessibleObject[0]), true);
        fields = Collections.unmodifiableMap(map);
    }

    @Override
    public void write(Kryo kryo, Output output, T object) {
        if(object == null) return;
        output.writeInt(version.value(), true);
        fields.forEach((field, since)->{
            try {
                kryo.writeClassAndObject(output, field.get(object));
            } catch(IllegalAccessException e) {
                throw new AssertionError(e);
            }
        });
    }

    @Override
    public T read(Kryo kryo, Input input, Class<T> type) {
        int version = input.readInt(true);
        T object = kryo.newInstance(type);
        fields.forEach((field, since)->{
            if(since.value() > version) return;
            try {
                field.set(object, kryo.readClassAndObject(input));
            } catch(IllegalAccessException e) {
                throw new AssertionError(e);
            }
        });
        return object;
    }
}
