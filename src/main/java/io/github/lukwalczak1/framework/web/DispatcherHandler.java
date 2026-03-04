package io.github.lukwalczak1.framework.web;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.lukwalczak1.framework.container.BeanFactory;
import io.github.lukwalczak1.framework.annotation.RequestMapping;
import io.github.lukwalczak1.framework.annotation.beans.Controller;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class DispatcherHandler implements HttpHandler {

    private final Map<String, MethodInvocation> routes = new HashMap<>();

    private final BeanFactory beanFactory;

    public DispatcherHandler(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        System.out.println("Initializing DispatcherHandler with BeanFactory: " + beanFactory);
        registerRoutes();
    }

    private void registerRoute(String path, String method, MethodInvocation invocation) {
        routes.put(method + ":" + path, invocation);
    }

    private void registerRoutes(){
        beanFactory.getRegisteredBeans().forEach( bean -> {
            if(bean.isAnnotationPresent(Controller.class)){
                String basePath = "";
                if(bean.isAnnotationPresent(RequestMapping.class)){
                    basePath = bean.getAnnotation(RequestMapping.class).value();
                }
                for( Method method : bean.getDeclaredMethods()){
                    if(method.isAnnotationPresent(RequestMapping.class)){
                        RequestMapping reqMapping = method.getAnnotation(RequestMapping.class);
                        String fullPath = basePath + reqMapping.value();
                        registerRoute(fullPath, reqMapping.method(), new MethodInvocation(beanFactory.getBean(bean), method));
                    }
                }
            }
        });
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("Handling request: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        MethodInvocation invocation = routes.get(method + ":" + path);
        if (invocation != null) {
            try {
                Object result = invocation.invoke();
                sendResponse(exchange, result);
            } catch (Exception e) {
                System.out.println("Error invoking method: " + e);
                sendResponse(exchange, 500, "Internal Server Error");
            }
        } else {
            sendResponse(exchange, 404, "Not Found");
        }
        exchange.close();
    }

    private void sendResponse(HttpExchange exchange, Object invocationResult) throws IOException {
        if( !(invocationResult instanceof ResponseEntity<?> responseEntity)){
            sendResponse(exchange, 200, invocationResult.toString());
            return;
        }
        if(responseEntity.getBody() instanceof String){
            exchange.sendResponseHeaders(responseEntity.getStatusCode(), responseEntity.getBody().toString().getBytes().length);
            exchange.getResponseBody().write(responseEntity.getBody().toString().getBytes());
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, -1);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
    }
}
