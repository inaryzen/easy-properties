package com.inaryzen.easyproperties;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Created by inaryzen on 10/6/2016.
 */
final class Metadata {
    private static final Map<Class<?>, Function<String, Object>> CONVERTERS = new HashMap<>();
    static {
        CONVERTERS.put(String.class, s -> s);
        CONVERTERS.put(Integer.class, Integer::parseInt);
        CONVERTERS.put(int.class, Integer::parseInt);
        CONVERTERS.put(Double.class, Double::parseDouble);
        CONVERTERS.put(double.class, Double::parseDouble);
        CONVERTERS.put(Boolean.class, Boolean::parseBoolean);
        CONVERTERS.put(boolean.class, Boolean::parseBoolean);
        CONVERTERS.put(long.class, Long::parseLong);
        CONVERTERS.put(Long.class, Long::parseLong);
        CONVERTERS.put(float.class, Float::parseFloat);
        CONVERTERS.put(Float.class, Float::parseFloat);
        CONVERTERS.put(short.class, Short::parseShort);
        CONVERTERS.put(Short.class, Short::parseShort);
        CONVERTERS.put(char.class, s -> s.charAt(0));
        CONVERTERS.put(Character.class, s -> s.charAt(0));
        CONVERTERS.put(byte.class, Byte::parseByte);
        CONVERTERS.put(Byte.class, Byte::parseByte);
    }

    private final String propertyFile;
    private final Map<String, Property> mapping;

    public Metadata(String propertyFile, Map<String, Property> mapping) {
        this.propertyFile = propertyFile;
        this.mapping = mapping;
    }

    public static <T> Metadata prepareMetadata(Class<T> settingsClass, String propertiesFile) {
        // form map: property name - getter & setter
        Map<String, Property> mapping = new HashMap<>();
        for (Method method : settingsClass.getMethods()) {
            String name = method.getName();
            String propertyName;
            if (name.startsWith("get")) {
                propertyName = name.substring("get".length()).toLowerCase();
                Property property = mapping.get(propertyName);
                if (property == null)
                    mapping.put(propertyName, property = new Property());
                property.getter = method;
            } else if (name.startsWith("set")) {
                propertyName = name.substring("set".length()).toLowerCase();
                Property property = mapping.get(propertyName);
                if (property == null)
                    mapping.put(propertyName, property = new Property());
                property.setter = method;
            } else if (name.startsWith("is")) {
                propertyName = name.substring("is".length()).toLowerCase();
                Property property = mapping.get(propertyName);
                if (property == null)
                    mapping.put(propertyName, property = new Property());
                property.getter = method;
            }
        }
        // remove incomplete properties
        Iterator<Property> itr = mapping.values().iterator();
        while (itr.hasNext()) {
            Property property = itr.next();
            if (property.getter == null || property.setter == null) {
                itr.remove();
            } else {
                Class<?> propertyType = property.getter.getReturnType();
                Function<String, Object> writeConverter = CONVERTERS.get(propertyType);
                if (writeConverter == null)
                    throw new RuntimeException("Unsupported property type: " + propertyType);

                property.setWriteConverter(writeConverter);
                property.setReadConverter(Objects::toString);
            }
        }
        return new Metadata(propertiesFile, mapping);
    }

    public String getPropertyFile() {
        return propertyFile;
    }

    public Map<String, Property> getMapping() {
        return mapping;
    }
}
