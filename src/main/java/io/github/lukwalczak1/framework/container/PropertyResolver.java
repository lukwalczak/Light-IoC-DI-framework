package io.github.lukwalczak1.framework.container;

import java.io.InputStream;
import java.util.Properties;

public class PropertyResolver {

    private final Properties properties = new Properties();

    public PropertyResolver(){
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")){
            properties.load(is);
        }catch (Exception e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }

    public String resolve(String key) {
        String cleanKey = key.replace("${", "").replace("}", "");
        return properties.getProperty(key);
    }
}
