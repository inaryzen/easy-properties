# easy-properties

This is simple properties manager intended to simplify using properties objects in 'helloworld' projects. 

###Usage:

1. Define a simple JavaBean class representing properties of the application. It should be public static/nested static class, all the properties of the class
should be accessible by getter/setter methods. Only primitive data types and their wrappers are supported as properties.


        public class Properties {
          private String path = "./data.out";
          private String description = "default description";
          private double width = 42.42;
          private int count = 33;

          public String getPath(){
            return path;
          }
          public void setPath(String path) {
            this.path = path;
          }
          public double getWidth() {
            return width;
          }
          public void setWidth(double width) {
            this.width = width;
          }

          // and so on...
        }


2. Use PropertiesManager.register() to read the properties file, instantiate properties object and initialize it with values from the file:

        public class HelloWorldApp() {
          private static final Properties properties = PropertiesManager.register(Properties.class, "app.properties");

          public static void main(String[] args) {
            System.out.println(properties.getPath());
          }
        }


3. Use PropertiesManager.save() in order to save the state of properties object back to the file:

          // ...
          public static void main(String[] args) {
            properties.setPath(".");
            properties.setDescription("here");
            PropertiesManager.save(properties);
          }
          // ...  

###Limitations:
- Works with one instance of the class only
- Doesn't support NULL values
