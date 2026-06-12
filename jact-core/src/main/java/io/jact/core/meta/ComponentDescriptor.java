package io.jact.core.meta;

import java.util.List;

public record ComponentDescriptor(String componentId, String beanClassName, String methodName, List<String> parameterTypeNames) {
    public ComponentDescriptor {
        parameterTypeNames = List.copyOf(parameterTypeNames);
    }

    public ComponentDescriptor(String beanClassName, String methodName) {
        this(beanClassName + "#" + methodName, beanClassName, methodName, List.of());
    }
}
