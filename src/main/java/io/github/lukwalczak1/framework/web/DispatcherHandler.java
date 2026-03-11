package io.github.lukwalczak1.framework.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.lukwalczak1.framework.annotation.beans.ControllerAdvice;
import io.github.lukwalczak1.framework.annotation.exception.ExceptionHandler;
import io.github.lukwalczak1.framework.annotation.web.RequestBody;
import io.github.lukwalczak1.framework.annotation.web.PathVariable;
import io.github.lukwalczak1.framework.container.BeanFactory;
import io.github.lukwalczak1.framework.annotation.web.RequestMapping;
import io.github.lukwalczak1.framework.annotation.beans.Controller;
import io.github.lukwalczak1.framework.interceptor.HandlerInterceptor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

public class DispatcherHandler implements HttpHandler {

    private final Map<String, MethodInvocation> routes = new HashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final BeanFactory beanFactory;

    private final List<HandlerInterceptor> handlerInterceptors = new ArrayList<>();

    public DispatcherHandler(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        handlerInterceptors.addAll(beanFactory.getBeansOfType(HandlerInterceptor.class));
        System.out.println("Initializing DispatcherHandler with BeanFactory: " + beanFactory);
        registerRoutes();
    }

    private void registerRoute(String path, String method, MethodInvocation invocation) {
        routes.put(method + ":" + path, invocation);
    }

    private void registerRoutes() {
        beanFactory.getRegisteredBeans().forEach(beanClass -> {
            if (beanClass.isAnnotationPresent(Controller.class)) {
                Object beanInstance = beanFactory.getBean(beanClass);
                Class<?> targetClass = beanClass.getName().contains("ByteBuddy")
                        ? beanClass.getSuperclass()
                        : beanClass;
                String basePath = "";
                if (targetClass.isAnnotationPresent(RequestMapping.class)) {
                    basePath = targetClass.getAnnotation(RequestMapping.class).value();
                }

                for (Method method : targetClass.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(RequestMapping.class)) {
                        RequestMapping reqMapping = method.getAnnotation(RequestMapping.class);
                        String fullPath = basePath + reqMapping.value();
                        registerRoute(fullPath, reqMapping.method(), new MethodInvocation(beanInstance, method));
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
            if(paramTypes[i] == String.class && method.getParameters()[i].isAnnotationPresent(PathVariable.class)){
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
        // Handler interceptors logic
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        MethodInvocation invocation = routes.get(method + ":" + path);
        if (invocation != null) {
            try {
                Object[] args = determineInvocationArgs(invocation, exchange);
                sendResponse(exchange, invocation.invoke(args));
            } catch (Exception e) {
                Throwable cause = e instanceof InvocationTargetException ? e.getCause() : e;
                Class<?> controllerClass = invocation.getInstance().getClass();
                if (controllerClass.getName().contains("ByteBuddy")) {
                    controllerClass = controllerClass.getSuperclass();
                }
                handleException(exchange, controllerClass, cause);
            }
        } else {
            sendResponse(exchange, 404, "Not Found");
        }
        exchange.close();
    }

    private List<Class<?>> getBeanClassesWithAnnotation(Class<? extends java.lang.annotation.Annotation> annotation) {
        return beanFactory.getRegisteredBeans().
                stream().filter(beanClass -> beanClass.isAnnotationPresent(annotation)).collect(Collectors.toList());
    }

    private void handleException(HttpExchange exchange, Class<?> controllerClass, Throwable exception) throws IOException {
        //Unwrap exception to find the root
        while(exception instanceof InvocationTargetException && exception.getCause() != null){
            exception = exception.getCause();
        }

        // Implementation in controller has priority over global handlers
        for( Method method : controllerClass.getDeclaredMethods()){
            if(method.isAnnotationPresent(ExceptionHandler.class)){
                Class<? extends Throwable> handledException = method.getAnnotation(ExceptionHandler.class).value();
                System.out.println(handledException.getName() + " vs " + exception.getClass().getName());
                if(handledException.isAssignableFrom(exception.getClass())){
                    try{
                        Object handlerInstance = beanFactory.getBean(controllerClass);
                        Object result = method.invoke(handlerInstance, exception);
                        if(result != null){
                            sendResponse(exchange, result);
                            return;
                        }
                    }catch (Exception e){
                        System.out.println("Error invoking exception handler: " + e);
                        sendResponse(exchange, 500, "Internal Server Error");
                        return;
                    }
                }

            }
        }
        List<Class<?>> controllerAdviceClasses = getBeanClassesWithAnnotation(ControllerAdvice.class);
        // global exception handlers
        for(Class<?> advice : controllerAdviceClasses){
            for(Method method : advice.getDeclaredMethods()){
                if(method.isAnnotationPresent(ExceptionHandler.class)){
                    Class<? extends Throwable> handledException = method.getAnnotation(ExceptionHandler.class).value();
                    if(handledException.isAssignableFrom(exception.getClass())){
                        try{
                            Object instance = beanFactory.getBean(advice);
                            Object result = method.invoke(instance, exception);
                            if(result != null){
                                sendResponse(exchange, result);
                                return;
                            }
                        }catch (Exception e){
                            sendResponse(exchange, 500, "Internal Server Error");
                            return;
                        }
                    }
                }
            }
        }


        //Unhandled exception, return generic error response
        sendResponse(exchange, 500, "Internal Server Error");
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
