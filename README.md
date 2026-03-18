# 🚀 Light-IoC-DI-Framework

A lightweight, custom Java framework inspired by Spring, implementing **Inversion of Control (IoC)** and **Dependency Injection (DI)** principles. This project was developed for educational purposes.

## 🌟 Key Features

* **Bean Management:** Automatic discovery and instantiation of classes marked with stereotype annotations and management of their lifecycle.
* **Dependency Injection:** Constructor and field injection with support for recursive dependency resolution.
* **Polymorphic DI:** Automatic mapping of interfaces to their respective implementations within the container.
* **AOP / Interceptors:** A "hook" system powered by **ByteBuddy Proxy**, allowing clean separation of cross-cutting concerns (e.g., logging, validation).
* **HTTP Routing:** Built-in `DispatcherHandler` integrated with `com.sun.net.httpserver`.
* **JSON Support:** Automatic object serialization and deserialization via the Jackson library.
* **External Configuration:** Support for injecting values from `.properties` files using `@Value` annotation.
* **Exception Handling:** Global exception handling mechanism using `@ControllerAdvice` and `@ExceptionHandler` annotations.
* **Bean Scopes:** Support for `@RequestScoped`, `@ApplicationScoped`, and `@Prototype` bean scopes, allowing fine-grained control over bean lifecycle and inst

---

## 🛠 Annotations

### Component Stereotypes
* `@Controller` – Marks classes that handle HTTP traffic.
* `@ControllerAdvice` – Global exception handling for controllers.
* `@Interceptor` – Components implementing cross-cutting logic.
* `@Repository` – Data Access Object (DAO) layer.
* `@Service` – Marks classes containing business logic.

### Dependency Injection
* `@Inject` – Marks the constructor that the framework should use for injection. If multiple constructors exist, the framework prioritizes the one with this annotation.
* `@Value("property.key")` – Injects values from external configuration files (e.g., `application.properties`).
* `@NotNull` – Indicates that a dependency must not be null, triggering an ValidationException if the framework fails to resolve it.

### Exception Handling
* `@ExceptionHandler(ExceptionClass.class)` – Marks methods that handle specific exceptions thrown by controllers

### Lifecycle & Scope
* `@RequestScoped` – Bean instance is created per HTTP request.
* `@ApplicationScoped` – Bean instance is shared across the entire application (default scope).
* `@Prototype` – A new bean instance is created every time it is requested.
* `@Lazy` – Bean is instantiated lazily, only when first requested.
* `@PreConstruct` – Method to be called after bean instantiation and dependency injection.
* `@PreDestroy` – Method to be called before bean destruction.

### Web & Routing
* `@RequestMapping(value, method)` – Defines the URI path and HTTP method (GET, POST, etc.).
* `@PathVariable` – Extracts data directly from the URI path.
* `@RequestBody` – Maps JSON from the request body to a Java object.

### Aspect-Oriented Programming (AOP)
* `@InterceptedBy(InterceptorClass.class)` – Marks methods to be intercepted by the specified interceptor.

---

## 🏗 System Architecture

The framework initialization follows these steps:

1.  **Scanning:** `ClassGraph` scans the base package for specific annotations.
2.  **Registration:** The framework builds a registry mapping interfaces to their implementations.
3.  **Instantiation:** `BeanFactory` creates the object graph, injecting dependencies via reflection.
4.  **Proxy Generation:** If a method has interceptor annotations, `ByteBuddy` generates a Proxy class that delegates calls to the original instance while wrapping them in interceptor logic.
5. **Lazy Initialization:** Beans marked with `@Lazy` are instantiated only when first requested, optimizing startup time and resource usage. 
6. **Request Handling:** `DispatcherHandler` matches incoming requests to methods and invokes them with the resolved arguments.
7. **Exception Handling:** If a controller method throws an exception, the framework checks for matching `@ExceptionHandler` methods in `@ControllerAdvice` classes and invokes them to generate an appropriate response.
8. **Lifecycle Management:** Beans with lifecycle annotations (`@PreConstruct`, `@PreDestroy`) have their respective methods invoked at the appropriate times during the bean lifecycle.
9. **Scope Management:** The framework manages bean scopes (e.g., `@RequestScoped`, `@ApplicationScoped`, `@Prototype`) to ensure correct instantiation and lifecycle based on the defined scope.
---

## 🚀 Usage Example

### Controller Definition with Interceptor
```java
@RequestScoped
@Controller
public class SimpleController {

    private final SimpleService simpleService;

    //Required for proxy generation when using Interceptors
    protected SimpleController() {
        this.simpleService = null; // For proxy generation
    }
    
    @Inject
    public SimpleController(SimpleService simpleService) {
        this.simpleService = simpleService;
    }
    
    @InterceptedBy(BasicInterceptor.class)
    @RequestMapping(value = "/api/hello", method = "GET")
    public ResponseEntity<String> sayHello(@PathVariable String path) {
        simpleService.executeBusinessLogic();
        return new ResponseEntity<>(200, "Success!");
    }
    
}
```

```java

@Service
public class SimpleService {
    public void executeBusinessLogic() {
        try {
            System.out.println("Executing business logic...");
        } catch (Exception e) {
            throw new ExampleException("An error occurred in business logic", e);
        }
    }


}
```
```java

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ExampleException.class)
    public ResponseEntity<String> handleExampleException(ExampleException ex) {
        return new ResponseEntity<>(500, "An error occurred: " + ex.getMessage());
    }
}

```


```java

@Interceptor
@RequestScoped
public class BasicInterceptor implements MethodInterceptor {
    public BasicInterceptor() {}
    
    @Override
    public Object invoke(InvocationContext c) throws Throwable {
        System.out.println("Before method invocation");
        Object result = c.proceed();
        System.out.println("After method invocation");
        return result;
    }
}
```
## 🛠 Tech Stack
* **Java 21**
* **ByteBuddy** (Dynamic Proxy generation)
* **ClassGraph** (Classpath scanning)
* **Jackson Databind** (JSON processing)
* **Maven** (Build tool)

---

## 🏗 Internal Logic: Proxy & Interceptors
The framework uses **ByteBuddy** to create dynamic proxies for classes that have methods annotated with `@InterceptedBy`. When such a method is invoked, the proxy intercepts the call and executes the associated interceptor logic before and after the actual method execution. This allows for clean separation of concerns, enabling features like logging, validation, or transaction management without cluttering business logic. 
1. The framework identifies methods annotated with `@InterceptedBy` during the scanning phase.
2. For each such method, a dynamic proxy is generated using ByteBuddy, which implements the same interface or extends the same class as the original bean.
3. The proxy intercepts method calls and delegates them to the specified interceptor(s) before invoking the original method logic.
4. Interceptors can be chained, allowing multiple interceptors to wrap around a single method


---

## ⚠️ Current Status
The project is under active development.
- [x] Constructor-based Injection.
- [x] Interface-to-Implementation resolution.
- [x] Dynamic Proxy mechanism for Interceptors.
- [x] JSON Response/Request handling.
- [x] `@Value` annotation for external configuration (`.properties`).
- [x] Exception Mapping (Global `@ExceptionHandler`).
- [x] Interceptor chain execution (multiple interceptors on a single method).
- [x] Lazy initialization for beans
- [x] Application Scope, Request Scope support and Prototype Scope.
- [x] Bean lifecycle callbacks (`@PostConstruct`, `@PreDestroy`).]
- [ ] View, Session Scope support.
- [ ] Circular dependency detection and resolution.
- [ ] Java-based configuration
- [ ] More complex Bean validation
- [ ] Handling multipart/form-data requests.
- [ ] Bean profiling for performance monitoring.
- [ ] @RequestParam for query parameters.
- [ ] Session management and cookie handling.
- [ ] Static resource handling (serving HTML/CSS/JS).
- [ ] Unit Testing (JUnit 5) for core components.
- [ ] Comprehensive documentation and usage examples.

---

## 📝 License
This project is open-source and available under the MIT License.