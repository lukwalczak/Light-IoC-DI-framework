package io.github.lukwalczak1.framework.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.lukwalczak1.framework.annotation.RequestBody;
import io.github.lukwalczak1.framework.annotation.RequestPath;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    public Object[] determineInvocationArgs(MethodInvocation invocation, HttpExchange exchange) throws IOException {
        Method method = invocation.getMethod();
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] args = new Object[paramTypes.length];
        String requestBody = exchange.getRequestBody() != null ? new String(exchange.getRequestBody().readAllBytes()) : "";
        String path = exchange.getRequestURI().getPath();
        for(int i = 0; i < paramTypes.length; i++){
            if(paramTypes[i] == String.class && method.getParameters()[i].isAnnotationPresent(RequestPath.class)){
                args[i] = path;
            }else if(method.getParameters()[i].isAnnotationPresent(RequestBody.class)){
                try {
                    args[i] = objectMapper.readValue(requestBody, paramTypes[i]);
                }catch (Exception e){
                    System.out.println("Error parsing request body: " + e);
                    args[i] = null;
                }
            }else{
                args[i] = null;
            }
        }
        return args;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("Handling request: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        MethodInvocation invocation = routes.get(method + ":" + path);
        if (invocation != null) {
            try {
                Object[] args = determineInvocationArgs(invocation, exchange);
                sendResponse(exchange, invocation.invoke(args));
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
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        if( invocationResult instanceof ResponseEntity<?> responseEntity){
            exchange.sendResponseHeaders(responseEntity.getStatusCode(), objectMapper.writeValueAsBytes(responseEntity.getBody()).length);
            exchange.getResponseBody().write(objectMapper.writeValueAsBytes(responseEntity.getBody()));
        }else{
            exchange.sendResponseHeaders(200, objectMapper.writeValueAsBytes(invocationResult).length);
            exchange.getResponseBody().write(objectMapper.writeValueAsBytes(invocationResult));
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
    }
}
