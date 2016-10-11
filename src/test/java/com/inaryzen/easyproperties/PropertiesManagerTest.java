package com.inaryzen.easyproperties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by inaryzen on 10/6/2016.
 */
public class PropertiesManagerTest {
    public static final String PROPERTIES_FILE = "app.properties";

    private static final String NAME_1 = "testing string";
    private static final int ID_1 = 42;
    private static final double PRICE_1 = 10.43;

    private static final String NAME_2 = "The price is 999.99!";
    private static final int ID_2 = 99;
    private static final double PRICE_2 = 999.99;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Creates regular property file, read by PropertiesManager, checks that all the values are read properly
     */
    @Test
    public void registerProperties() throws Exception {
        Path tmpFolder = folder.getRoot().toPath();
        Path propertiesFile = tmpFolder.resolve(PROPERTIES_FILE).toAbsolutePath();

        File file = folder.newFile(PROPERTIES_FILE);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("id=" + ID_2 + "\n");
            writer.write("price=" + PRICE_2 + "\n");
            writer.write("name=" + NAME_2 + "\n");
            writer.flush();
        }

        PropertiesManager manager = new PropertiesManager();
        PropertiesBean properties = manager.registerProperties(PropertiesBean.class, propertiesFile.toString());
        assertEquals(ID_2, properties.getId());
        assertEquals(NAME_2, properties.getName());
        assertEquals(PRICE_2, properties.getPrice(), 0.0);
    }

    /**
     * Creates incomplete property file, read by PropertiesManager, checks that all the values are read properly
     * and values that are not presented in the file are initialized by default values
     */
    @Test
    public void incompletePropertiesFile() throws Exception {
        Path tmpFolder = folder.getRoot().toPath();
        Path propertiesFile = tmpFolder.resolve(PROPERTIES_FILE).toAbsolutePath();

        File file = folder.newFile(PROPERTIES_FILE);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("name=" + NAME_2 + "\n");
            writer.flush();
        }

        PropertiesManager manager = new PropertiesManager();
        PropertiesBean properties = manager.registerProperties(PropertiesBean.class, propertiesFile.toString());
        assertEquals(ID_1, properties.getId());
        assertEquals(NAME_2, properties.getName());
        assertEquals(PRICE_1, properties.getPrice(), 0.0);
    }

    /**
     * Creates property file populated by invalid values, read by PropertiesManager,
     * checks that all the properties are initialized by default values
     */
    @Test
    public void readInvalidProperties() throws Exception {
        Path tmpFolder = folder.getRoot().toPath();
        Path propertiesFile = tmpFolder.resolve(PROPERTIES_FILE).toAbsolutePath();

        File file = folder.newFile(PROPERTIES_FILE);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("value1=" + NAME_2 + "\n");
            writer.write("value2=" + NAME_2 + "\n");
            writer.write("value3=" + NAME_2 + "\n");
            writer.flush();
        }

        PropertiesManager manager = new PropertiesManager();
        PropertiesBean properties = manager.registerProperties(PropertiesBean.class, propertiesFile.toString());
        assertEquals(ID_1, properties.getId());
        assertEquals(NAME_1, properties.getName());
        assertEquals(PRICE_1, properties.getPrice(), 0.0);
    }

    /**
     * Do not create property file, register property class in PropertiesManager, checks that all the values
     * are populated by default values
     */
    @Test
    public void defaultPropertyValues() throws Exception {
        Path tmpFolder = folder.getRoot().toPath();
        Path propertiesFile = tmpFolder.resolve(PROPERTIES_FILE).toAbsolutePath();

        PropertiesManager manager = new PropertiesManager();
        PropertiesBean properties = manager.registerProperties(PropertiesBean.class, propertiesFile.toString());
        assertEquals(ID_1, properties.getId());
        assertEquals(NAME_1, properties.getName());
        assertEquals(PRICE_1, properties.getPrice(), 0.0);
    }

    /**
     * Do not create property file, create property instance by PropertiesManager, changes some values, saves
     * properties by PropertiesManager in property file, load them by another instance of PropertiesManager
     * checks that all the values were stored properly.
     */
    @Test
    public void saveProperties() throws Exception {
        Path tmpFolder = folder.getRoot().toPath();
        Path propertiesFile = tmpFolder.resolve(PROPERTIES_FILE).toAbsolutePath();

        PropertiesManager manager = new PropertiesManager();
        PropertiesBean properties = manager.registerProperties(PropertiesBean.class, propertiesFile.toString());
        properties.setName(NAME_2);
        properties.setId(ID_2);
        manager.saveProperties(properties);
        assertTrue(Files.exists(propertiesFile));

        manager = new PropertiesManager();
        properties = manager.registerProperties(PropertiesBean.class, propertiesFile.toString());
        assertEquals(ID_2, properties.getId());
        assertEquals(NAME_2, properties.getName());
        assertEquals(PRICE_1, properties.getPrice(), 0.0);
    }

    /**
     * Register new property instance, saves null value for a field, tries to execute saveProperties
     */
    @Test
    public void nullValuesException() throws Exception {
        Path tmpFolder = folder.getRoot().toPath();
        Path propertiesFile = tmpFolder.resolve(PROPERTIES_FILE).toAbsolutePath();

        PropertiesManager manager = new PropertiesManager();
        PropertiesBean properties = manager.registerProperties(PropertiesBean.class, propertiesFile.toString());
        properties.setName(null);

        thrown.expect(RuntimeException.class);
        manager.saveProperties(properties);
    }

    /**
     * Register a new property instance for the class that has properties of unsupported types
     */
    @Test
    public void invalidPropertyType() throws Exception {
        Path tmpFolder = folder.getRoot().toPath();
        Path propertiesFile = tmpFolder.resolve(PROPERTIES_FILE).toAbsolutePath();

        PropertiesManager manager = new PropertiesManager();
        thrown.expect(RuntimeException.class);
        InvalidPropertyBean properties = manager.registerProperties(InvalidPropertyBean.class, propertiesFile.toString());
        properties.setBigId(new BigInteger("42"));
    }

    public static class PropertiesBean {
        private int id = ID_1;
        private String name = NAME_1;
        private double price = PRICE_1;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }
    }

    private static class InvalidPropertyBean {
        private BigInteger bigId = new BigInteger("1000");

        public BigInteger getBigId() {
            return bigId;
        }

        public void setBigId(BigInteger bigId) {
            this.bigId = bigId;
        }
    }

}