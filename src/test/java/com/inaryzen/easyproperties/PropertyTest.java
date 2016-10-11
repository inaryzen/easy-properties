package com.inaryzen.easyproperties;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by inaryzen on 10/10/2016.
 */
public class PropertyTest {
    @Test
    public void write() throws Exception {
        Property property = new Property();
        property.setter = TestClass.class.getMethod("setPrice", double.class);
        property.setWriteConverter(Double::parseDouble);

        TestClass testClass = new TestClass();
        property.write(testClass, "42.999");

        Assert.assertEquals(42.999, testClass.getPrice(), 0.0);
    }

    @Test
    public void read() throws Exception {
        Property property = new Property();
        property.getter = TestClass.class.getMethod("getPrice");
        property.setReadConverter(d -> Double.toString((Double)d));

        TestClass testClass = new TestClass();
        testClass.setPrice(42.999);
        String price = property.read(testClass);

        Assert.assertEquals("42.999", price);
    }

    public static class TestClass {
        private double price;

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }
    }
}