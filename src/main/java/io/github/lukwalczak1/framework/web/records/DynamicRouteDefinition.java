package io.github.lukwalczak1.framework.web.records;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DynamicRouteDefinition {
    private final String method;
    private final String pathPattern;
    private final RouteDefinition routeDefinition;
    private final List<String> paramNames;

    public DynamicRouteDefinition(String method, String pathPattern, RouteDefinition routeDefinition) {
        this.method = method;
        this.pathPattern = pathPattern;
        this.routeDefinition = routeDefinition;
        this.paramNames = this.extractParamNames(pathPattern);
    }

    private List<String> extractParamNames(String pathPattern){
        List<String> ParamNames = new ArrayList<>();
        Matcher m = Pattern.compile("\\{([^}]+)\\}").matcher(pathPattern);
        while (m.find()) {
            ParamNames.add(m.group(1));
        }
        return ParamNames;
    }

    public boolean matches(String method, String path) {
        if (!this.method.equalsIgnoreCase(method)) {
            return false;
        }
        String regex = pathPattern.replaceAll("\\{[^}]+\\}", "([^/]+)");
        return path.matches(regex);
    }

    public Map<String, String> extractPathVariables(String path) {
        Map<String, String> pathVariables = new HashMap<>();
        String regex = pathPattern.replaceAll("\\{[^}]+\\}", "([^/]+)");
        Matcher m = Pattern.compile(regex).matcher(path);
        if (m.matches()) {
            for (int i = 0; i < paramNames.size(); i++) {
                pathVariables.put(paramNames.get(i), m.group(i + 1));
            }
        }
        return pathVariables;
    }

    public RouteDefinition getRouteDefinition() {
        return routeDefinition;
    }

}
