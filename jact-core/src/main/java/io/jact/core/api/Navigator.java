package io.jact.core.api;

import io.jact.core.routing.RouteParams;
import io.jact.core.routing.RouteTemplate;

public interface Navigator {
    void push(String path);

    default void push(RouteTemplate template, RouteParams params) {
        push(template.resolve(params));
    }

    void replace(String path);

    default void replace(RouteTemplate template, RouteParams params) {
        replace(template.resolve(params));
    }

    boolean back();

    String currentPath();

    RouteParams currentParams();
}
