package io.jact.core.runtime.spi;

import io.jact.annotations.JNode;
import io.jact.core.runtime.RenderRequest;

@FunctionalInterface
public interface PageResolver {
    JNode resolve(RenderRequest request);
}
