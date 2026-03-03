package io.github.lukwalczak1.framework.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.lukwalczak1.framework.container.BeanFactory;
import io.github.lukwalczak1.framework.annotation.RequestMapping;
import io.github.lukwalczak1.framework.annotation.beans.Controller;
import java.io.IOException;
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
                String response = result != null ? result.toString() : "";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                exchange.getResponseBody().write(response.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
                String response = "Internal Server Error";
                exchange.sendResponseHeaders(500, response.getBytes().length);
                exchange.getResponseBody().write(response.getBytes());
            }
        } else {
            String response = "Not Found";
            exchange.sendResponseHeaders(404, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
        }
        exchange.close();
    }
}
