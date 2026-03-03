package io.github.lukwalczak1.framework.web;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpServerManager {

    HttpServer httpServer;

    DispatcherHandler dispatcherHandler;

    public HttpServerManager(int port, DispatcherHandler handler){
        this.dispatcherHandler = handler;
        try{
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        }catch (IOException e){
            System.out.println("Cannot create " + e);
        }
    }

    public void startHttpServer(DispatcherHandler dispatcherHandler){
        httpServer.createContext("/", dispatcherHandler);
        httpServer.setExecutor(null);
        httpServer.start();
    }

}
