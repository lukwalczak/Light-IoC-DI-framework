package io.github.lukwalczak1.framework.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.lukwalczak1.framework.container.annotations.beans.ControllerAdvice;
import io.github.lukwalczak1.framework.exception.annotations.ExceptionHandler;
import io.github.lukwalczak1.framework.web.annotations.RequestBody;
import io.github.lukwalczak1.framework.web.annotations.PathVariable;
import io.github.lukwalczak1.framework.container.BeanFactory;
import io.github.lukwalczak1.framework.web.annotations.RequestMapping;
import io.github.lukwalczak1.framework.container.annotations.beans.Controller;
import io.github.lukwalczak1.framework.web.response.ResponseCodes;
import io.github.lukwalczak1.framework.web.response.ResponseEntity;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import static io.github.lukwalczak1.framework.web.response.ResponseCodes.INTERNAL_SERVER_ERROR;
import static io.github.lukwalczak1.framework.web.response.ResponseCodes.NOT_FOUND;

public class DispatcherHandler implements HttpHandler {

    private final Map<String, RouteDefinition> routes = new HashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final BeanFactory beanFactory;

    public DispatcherHandler(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        registerRoutes();
    }

    private void registerRoute(String path, String method, RouteDefinition routeDefinition) {
        routes.put(method + ":" + path, routeDefinition);
    }

    private void registerRoutes() {
        beanFactory.getBeanClasses().forEach(beanClass -> {
            if (!beanClass.isAnnotationPresent(Controller.class)) {
                return;
            }
            String basePath = "";
            if (beanClass.isAnnotationPresent(RequestMapping.class)) {
                basePath = beanClass.getAnnotation(RequestMapping.class).value();
            }

            for (Method method : beanClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(RequestMapping.class)) {
                    RequestMapping reqMapping = method.getAnnotation(RequestMapping.class);
                    String fullPath = basePath + reqMapping.value();
                    registerRoute(fullPath, reqMapping.method(), new RouteDefinition(beanClass, method));
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
        RouteDefinition route = routes.get(method + ":" + path);
        if (route == null) {
            sendResponse(exchange, NOT_FOUND);
            return;
        }
        try {
            Object controllerInstance = beanFactory.getBean(route.controllerClass());

            MethodInvocation invocation = new MethodInvocation(controllerInstance, route.method());
            Object[] args = determineInvocationArgs(invocation, exchange);

            sendResponse(exchange, invocation.invoke(args));
        }catch (InvocationTargetException e) {
            Throwable realCause = e.getCause();
            realCause.printStackTrace();
            sendResponse(exchange, 500);
        } catch (Exception e) {
            Throwable cause = e instanceof InvocationTargetException ? e.getCause() : e;
            Class<?> controllerClass = route.controllerClass();
            if (controllerClass.getName().contains("ByteBuddy")) {
                controllerClass = controllerClass.getSuperclass();
            }
            handleException(exchange, controllerClass, cause);
        }finally {
            // Clear request-scoped beans after each request
            beanFactory.getRequestScope().clear();
        }
        exchange.close();
    }

    private List<Class<?>> getBeanClassesWithAnnotation(Class<? extends java.lang.annotation.Annotation> annotation) {
        return beanFactory.getRegisteredBeans().
                stream().filter(beanClass -> beanClass.isAnnotationPresent(annotation)).collect(Collectors.toList());
    }

    private void handleException(HttpExchange exchange, Class<?> controllerClass, Throwable exception) throws IOException {
        // Unwrap exception to find the root
        while(exception instanceof InvocationTargetException && exception.getCause() != null){
            exception = exception.getCause();
        }

        // Implementation in controller has priority over global handlers
        if(controllerExceptionHandler(controllerClass, exception, exchange)){
            return;
        }

        // If controller didn't handle the exception, we look for global handlers in @ControllerAdvice classes
        List<Class<?>> controllerAdviceClasses = getBeanClassesWithAnnotation(ControllerAdvice.class);
        if(globalExceptionHandler(controllerAdviceClasses, exception, exchange)){
            return;
        }

        //Unhandled exception, return generic error response
        sendResponse(exchange, INTERNAL_SERVER_ERROR);
    }

    private boolean controllerExceptionHandler(Class<?> controllerClass, Throwable exception, HttpExchange exchange) throws IOException {
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
                            return true;
                        }
                    }catch (Exception e){
                        System.out.println("Error invoking exception handler: " + e);
                        sendResponse(exchange, INTERNAL_SERVER_ERROR);
                        return true;
                    }
                }

            }
        }
        return false;
    }

    //Returns true if exception was handled and response was sent, false otherwise
    private boolean globalExceptionHandler(List<Class<?>> controllerAdviceClasses, Throwable exception, HttpExchange exchange) throws IOException {
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
                                return true;
                            }
                        }catch (Exception e){
                            // If exception handler itself throws an exception, we log it and return generic error response
                            sendResponse(exchange, INTERNAL_SERVER_ERROR);
                            System.out.println("Error invoking global exception handler: " + e);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
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

    private void sendResponse(HttpExchange exchange, ResponseCodes response) throws IOException {
        exchange.sendResponseHeaders(response.getCode(), response.getDescription().getBytes().length);
        exchange.getResponseBody().write(response.getDescription().getBytes());
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
    }
}
