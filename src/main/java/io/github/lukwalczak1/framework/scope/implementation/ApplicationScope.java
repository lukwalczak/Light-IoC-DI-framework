package io.github.lukwalczak1.framework.scope.implementation;

import io.github.lukwalczak1.framework.scope.interfaces.AbstractScope;

/**
 * ApplicationScope is a singleton scope that creates a single instance of a bean for the entire application.
 * This instance is shared across all requests and sessions.
 */
public class ApplicationScope extends AbstractScope {

}
