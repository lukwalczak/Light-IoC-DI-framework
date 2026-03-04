package io.github.lukwalczak1;

import io.github.lukwalczak1.framework.ApplicationContext;

public class Main {
    public static void main(String[] args) throws Exception{
        ApplicationContext appContext = ApplicationContext.getInstance();
        appContext.startApplication(8080, "io.github.lukwalczak1");
    }

}