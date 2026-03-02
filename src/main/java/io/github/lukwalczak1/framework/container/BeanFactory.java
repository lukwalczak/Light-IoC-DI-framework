package io.github.lukwalczak1.framework.container;

import java.util.*;
import java.lang.reflect.*;

import io.github.classgraph.*;


public class BeanFactory {
    private static BeanFactory instance;

    private Map<Class<?>, Object> beans = new HashMap<>();

    private List<Class<?>> beansClassInfo = new ArrayList<>();

    private static String basePackage = "io.github.lukwalczak1";

    public static BeanFactory getInstance() {
        if (instance == null) {
            instance = new BeanFactory();
        }
        return instance;
    }

    public void registerBean(Class<?> objectClass, Object instance) {
        beans.put(objectClass, instance);
    }

    public Set<Class<?>> getRegisteredBeans() {
        return beans.keySet();
    }

    public <T> T getBean(Class<T> objectClass){
        return (T) beans.get(objectClass);
    }

    public void scanBeansForDependencies(){
        for( Class<?> clazz : beansClassInfo){
            Constructor<?>[] classConstructors = clazz.getConstructors();
            for(Constructor<?> constructor : classConstructors){
                if(constructor.isAnnotationPresent(io.github.lukwalczak1.framework.annotation.Inject.class)){
                    Parameter[] parameters = constructor.getParameters();
                    List<Object> dependencies = new ArrayList<>();
            }
        }
    }}

    public void scanForBeans(){
        List<String> annotationNames = scanForAvailableAnnotations();
        System.out.println("Found annotations: " + annotationNames);
        for(String annotationName : annotationNames){
                try(ScanResult scanResult = new ClassGraph()
                        .acceptPackages(basePackage)
                        .enableClassInfo()
                        .enableAnnotationInfo()
                        .scan()){
                    ClassInfoList annotatedClasses = scanResult.getClassesWithAnnotation(annotationName);
                    annotatedClasses.forEach(classInfo->{
                        try{
                            Class<?> clazz = classInfo.loadClass();
                            beansClassInfo.add(clazz);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    });
                }
        }
    }

    public List<String> scanForAvailableAnnotations() {
        try(ScanResult scanResult = new ClassGraph()
                .acceptPackages(basePackage)
                .enableClassInfo()
                .scan()) {
            return scanResult
                    .getAllClasses()
                    .filter(ClassInfo::isAnnotation)
                    .getNames();
        }catch (Exception e){
            e.printStackTrace();
            return List.of();
        }

    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

}
