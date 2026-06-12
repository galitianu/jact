package io.jact.core.meta;

import java.util.List;

public record PageDescriptor(String routeTemplate, String beanClassName, String methodName, List<String> parameterTypeNames) {
    public PageDescriptor {
        parameterTypeNames = List.copyOf(parameterTypeNames);
    }

    public PageDescriptor(String routeTemplate, String beanClassName, String methodName) {
        this(routeTemplate, beanClassName, methodName, List.of());
    }
}
