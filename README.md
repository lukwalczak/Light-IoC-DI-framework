# 🚀 Light-IoC-DI-Framework

A lightweight, custom Java framework inspired by Spring, implementing **Inversion of Control (IoC)** and **Dependency Injection (DI)** principles. This project was developed for educational purposes.

## 🌟 Key Features

* **Bean Management:** Automatic discovery and instantiation of classes marked with stereotype annotations (Singleton scope).
* **Dependency Injection:** Constructor-based injection with support for recursive dependency resolution.
* **Polymorphic DI:** Automatic mapping of interfaces to their respective implementations within the container.
* **AOP / Interceptors:** A "hook" system powered by **ByteBuddy Proxy**, allowing clean separation of cross-cutting concerns (e.g., logging, validation).
* **HTTP Routing:** Built-in `DispatcherHandler` integrated with `com.sun.net.httpserver`.
* **JSON Support:** Automatic object serialization and deserialization via the Jackson library.

---

## 🛠 Annotations

### Component Stereotypes
* `@Controller` – Marks classes that handle HTTP traffic.
* `@Service` – Marks classes containing business logic.
* `@Repository` – Data Access Object (DAO) layer.
* `@Interceptor` – Components implementing cross-cutting logic.

### Dependency Injection
* `@Inject` – Marks the constructor that the framework should use for injection. If multiple constructors exist, the framework prioritizes the one with this annotation.

### Web & Routing
* `@RequestMapping(value, method)` – Defines the URI path and HTTP method (GET, POST, etc.).
* `@PathVariable` – Extracts data directly from the URI path.
* `@RequestBody` – Maps JSON from the request body to a Java object.

### Aspect-Oriented Programming (AOP)
* `@PreInvoke(InterceptorClass.class)` – Executes the `intercept(Object[] args)` method before the controller method runs.
* `@PostInvoke(InterceptorClass.class)` – Executes interceptor logic after the method finishes.

---

## 🏗 System Architecture

The framework initialization follows these steps:

1.  **Scanning:** `ClassGraph` scans the base package for specific annotations.
2.  **Registration:** The framework builds a registry mapping interfaces to their implementations.
3.  **Instantiation:** `BeanFactory` creates the object graph, injecting dependencies via reflection.
4.  **Proxy Generation:** If a method has interceptor annotations, `ByteBuddy` generates a Proxy class that delegates calls to the original instance while wrapping them in interceptor logic.
5.  **Request Handling:** `DispatcherHandler` matches incoming requests to methods and invokes them with the resolved arguments.

---

## 🚀 Usage Example

### Controller Definition with Interceptor
```java
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

    @PreInvoke(BasicInterceptor.class)
    @RequestMapping(value = "/api/hello", method = "GET")
    public ResponseEntity<String> sayHello(@PathVariable String path) {
        simpleService.executeBusinessLogic();
        return new ResponseEntity<>(200, "Success!");
    }
}
```
```java
@Interceptor
public class BasicInterceptor {
    public void intercept(Object[] args) {
        System.out.println("Intercepted call with arguments: " + Arrays.toString(args));
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
The framework uses a **Delegation Pattern** via ByteBuddy. When an interceptor is detected:
1. An original instance of the bean is created (with all dependencies injected).
2. A Proxy subclass is generated.
3. The Proxy intercepts method calls, executes `@PreInvoke` logic, and then explicitly calls the method on the **original instance**.
4. This ensures that even if the Proxy has null fields (due to ByteBuddy's subclassing), the business logic always runs on a fully initialized object.



---

## ⚠️ Current Status
The project is under active development.
- [x] Constructor-based Injection.
- [x] Interface-to-Implementation resolution.
- [x] Dynamic Proxy mechanism for Interceptors.
- [x] JSON Response/Request handling.
- [ ] `@Value` annotation for external configuration (`.properties`).
- [ ] Static resource handling (serving HTML/CSS/JS).
- [ ] Exception Mapping (Global `@ExceptionHandler`).

---

## 📝 License
This project is open-source and available under the MIT License.