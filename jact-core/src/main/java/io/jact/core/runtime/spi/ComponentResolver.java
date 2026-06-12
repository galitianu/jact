package io.jact.core.runtime.spi;

import io.jact.annotations.JNode;
import io.jact.core.runtime.ComponentRequest;

@FunctionalInterface
public interface ComponentResolver {
    JNode resolve(ComponentRequest request);
}
