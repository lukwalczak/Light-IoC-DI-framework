package io.github.lukwalczak1.framework;

import io.github.lukwalczak1.framework.container.BeanFactory;
import io.github.lukwalczak1.framework.web.DispatcherHandler;
import io.github.lukwalczak1.framework.web.HttpServerManager;

public class ApplicationContext {

    private static ApplicationContext instance;

    private BeanFactory beanFactory;

    private DispatcherHandler dispatcherHandler;

    private HttpServerManager httpServerManager;

    private ApplicationContext() {
    }

    public static ApplicationContext getInstance() {
        if (instance == null) {
            instance = new ApplicationContext();
        }
        return instance;
    }

    public void startApplication(int port, String basePackage) {
        beanFactory = BeanFactory.getInstance();
        beanFactory.initContext(basePackage);
        dispatcherHandler = new DispatcherHandler(beanFactory);
        HttpServerManager serverManager = new HttpServerManager(8080, dispatcherHandler);
        serverManager.startHttpServer(dispatcherHandler);
    }


}
