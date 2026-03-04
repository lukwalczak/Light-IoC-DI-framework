package io.github.lukwalczak1;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.lukwalczak1.framework.ApplicationContext;
import io.github.lukwalczak1.framework.container.BeanFactory;
import io.github.lukwalczak1.framework.web.DispatcherHandler;
import io.github.lukwalczak1.framework.web.HttpServerManager;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws Exception{
        ApplicationContext appContext = ApplicationContext.getInstance();
        appContext.startApplication(8080, "io.github.lukwalczak1");
    }

}