package io.github.lukwalczak1.framework.container;

import java.util.*;
import java.lang.reflect.*;

import io.github.classgraph.*;


public class BeanFactory {
    private static BeanFactory instance;

    private Map<Class<?>, Object> beans = new HashMap<>();

    private static String basePackage = "io.github.lukwalczak1";

    public static BeanFactory getInstance() {
        if (instance == null) {
            instance = new BeanFactory();
        }
        return instance;
    }

    public void initContext(String basePackage) {
        scanForBeans();
    }

    private void registerBean(Class<?> objectClass, Object instance) {
        beans.put(objectClass, instance);
        System.out.println("Registered bean: " + objectClass.getName());
    }

    public Set<Class<?>> getRegisteredBeans() {
        return beans.keySet();
    }

    public <T> T getBean(Class<T> objectClass) {
        return (T) beans.get(objectClass);
    }

    private <T> T getOrCreateBean(Class<T> objectClass){
        if(beans.containsKey(objectClass)){
            return objectClass.cast(beans.get(objectClass));
        }
        try{
            Constructor<?> constructor = objectClass.getDeclaredConstructors()[0];
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] parameters = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                parameters[i] = getOrCreateBean(parameterTypes[i]);
            }
            T instance = (T) constructor.newInstance(parameters);
            registerBean(objectClass, instance);
            return instance;
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("Failed to create bean for class: " + objectClass.getName());
        }
    }


    private void scanForBeans() {
        List<String> annotationNames = scanForAvailableAnnotations();
        System.out.println("Found annotations: " + annotationNames);
        try (ScanResult scanResult = new ClassGraph()
                .acceptPackages(basePackage)
                .enableClassInfo()
                .enableAnnotationInfo()
                .scan()) {
            Set<Class<?>> annotatedClasses = new HashSet<>();
            annotationNames.forEach( annotationName -> {
               scanResult.getAllClasses()
                       .filter(classInfo -> classInfo.hasAnnotation(annotationName))
                       .forEach(classInfo -> {
                           annotatedClasses.add(classInfo.loadClass());
                       });
            });
            System.out.println("Found annotated classes: " + annotatedClasses);
            annotatedClasses.forEach(this::getOrCreateBean);
        }
    }

    public List<String> scanForAvailableAnnotations() {
        try (ScanResult scanResult = new ClassGraph()
                .acceptPackages("io.github.lukwalczak1.framework.annotation.beans")
                .enableClassInfo()
                .scan()) {
            return scanResult
                    .getAllClasses()
                    .filter(ClassInfo::isAnnotation)
                    .getNames();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }

    }

}
