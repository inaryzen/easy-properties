package com.inaryzen.easyproperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Implementation notes:
 * - does not support NULL values
 * - can handle only one instance of an every class.
 * <p>
 * Created by inaryzen on 9/23/2016.
 */
public class PropertiesManager {
    private static final PropertiesManager singleton = new PropertiesManager();

    private final Map<Class<?>, Metadata> registry = new HashMap<>();

    public static synchronized void save(Object instance) {
        singleton.saveProperties(instance);
    }

    public static synchronized <T> T register(Class<T> settingsClass, String propertiesFile) {
        return singleton.registerProperties(settingsClass, propertiesFile);
    }

    public <T> T registerProperties(Class<T> settingsClass, String propertiesFile) {
        Objects.requireNonNull(settingsClass);
        Objects.requireNonNull(propertiesFile);

        if (registry.containsKey(settingsClass))
            throw new IllegalArgumentException("Class " + settingsClass + " has been already registered");

        Metadata metadata = Metadata.prepareMetadata(settingsClass, propertiesFile);
        registry.put(settingsClass, metadata);

        T instance = createNewInstance(settingsClass);

        Path path = Paths.get(propertiesFile);
        if (Files.notExists(path))
            return instance;

        // load properties from disk
        InputStream inputStream;
        try {
            inputStream = Files.newInputStream(path, StandardOpenOption.READ);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read file with properties: " + propertiesFile, e);
        }
        Properties properties = new Properties();
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read file with properties: " + propertiesFile, e);
        }

        // move data from properties to settings class
        for (Map.Entry<String, Property> entry : metadata.getMapping().entrySet()) {
            String propertyName = entry.getKey();
            Property property = entry.getValue();

            String value = properties.getProperty(propertyName);

            // if there is no such property, just omit it
            if (value == null)
                continue;

            property.write(instance, value);
        }
        return instance;
    }

    public void saveProperties(Object instance) {
        Objects.requireNonNull(instance);

        Class<?> settingsClass = instance.getClass();
        Metadata metadata = registry.get(settingsClass);

        Properties properties = new Properties();
        for (Map.Entry<String, Property> entry : metadata.getMapping().entrySet()) {
            String propertyName = entry.getKey();
            Property property = entry.getValue();

            String value = property.read(instance);
            properties.setProperty(propertyName, value);
        }

        String propertyFile = metadata.getPropertyFile();
        Path path = Paths.get(propertyFile);
        OutputStream outputStream;
        try {
            outputStream = Files.newOutputStream(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write to file : " + propertyFile, e);
        }

        try {
            properties.store(outputStream, "");
        } catch (IOException e) {
            throw new RuntimeException("Cannot write to file : " + propertyFile, e);
        }
    }

    @SuppressWarnings("unchecked") // cast of the result
    private static <T> T createNewInstance(Class<T> settingsClass) {
        Constructor<?>[] declaredConstructors = settingsClass.getDeclaredConstructors();
        Constructor<?> defaultConstructor = null;
        for (Constructor<?> declaredConstructor : declaredConstructors) {
            if (declaredConstructor.getParameterCount() == 0) {
                defaultConstructor = declaredConstructor;
                break;
            }
        }
        if (defaultConstructor == null) {
            throw new RuntimeException("Non-argument constructor not found; Possible solutions: 1) add a non-argument " +
                    "constructor to the class 2) if the class is nested, define it as a static one.");
        }

        handleNestedPrivateClass(settingsClass, defaultConstructor);

        Object result;
        try {
            result = defaultConstructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Cannot create an instance of class: " + settingsClass, e);
        }

        return (T) result;
    }

    /**
     * In accordance with JLS (6.6.1. Determining Accessibility) we are allowed to access a private constructor of
     * a nested class if the access occurs from within the body of the top level class. Constructor#newInstance does not
     * respect this rule, so we need to handle it be ourselves using AccessibleObject#setAccessible.
     */
    private static <T> void handleNestedPrivateClass(Class<T> settingsClass, Constructor<?> defaultConstructor) {
        if (settingsClass.getEnclosingClass() != null && Modifier.isPrivate(defaultConstructor.getModifiers())) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String managerClassName = PropertiesManager.class.getName();
            String callerClassName = null;
            boolean gate = false;
            // looking for the class in the stack that comes after PropertiesManager part
            // 1) finds the beginning of PropertiesManager part (gate == false)
            // 2) finds the first class after (gate == true)
            for (StackTraceElement stackTraceElement : stackTrace) {
                String className = stackTraceElement.getClassName();
                if (!gate) {
                    if (managerClassName.equals(className))
                        gate = true;
                } else if (!managerClassName.equals(className)) {
                    callerClassName = className;
                    break;
                }
            }
            if (callerClassName == null)
                throw new RuntimeException("Cannot identify caller's name");

            Class<?> enclosingClass;
            try {
                enclosingClass = Class.forName(callerClassName);
            } catch (ClassNotFoundException e) {
                String message = String.format("Failed to check if class '%s' is an enclosing class for property " +
                                "class '%s': class not found. The possible solution: declare '%s' and its constructor as public",
                        callerClassName, settingsClass.getName(), settingsClass.getName());
                throw new RuntimeException(message, e);
            }

            if (settingsClass.getEnclosingClass() == enclosingClass) {
                defaultConstructor.setAccessible(true);
            }
        }
    }
}
