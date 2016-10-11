package com.inaryzen.easyproperties;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Created by inaryzen on 10/6/2016.
 */
final class Property {
    public Method getter;
    public Method setter;
    private Function<String, Object> writeConverter;
    private Function<Object, String> readConverter;

    public void write(Object instance, String value) {
        if ("null".equalsIgnoreCase(value))
            throw new RuntimeException("The implementation doesn't support null values");

        Object parameter = writeConverter.apply(value);
        try {
            setter.invoke(instance, parameter);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Cannot apply property value; method: " + setter + "; value: " + parameter, e);
        }
    }

    public String read(Object object) {
        Object value;
        try {
            value = getter.invoke(object);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Cannot invoke method: " + getter, e);
        }
        if (value == null)
            throw new RuntimeException("The implementation doesn't support null values");
        return readConverter.apply(value);
    }

    public void setWriteConverter(Function<String, Object> writeConverter) {
        this.writeConverter = writeConverter;
    }

    public void setReadConverter(Function<Object, String> readConverter) {
        this.readConverter = readConverter;
    }
}
