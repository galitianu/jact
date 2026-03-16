package io.jact.core;

import io.jact.annotations.JNode;
import io.jact.core.descriptor.PageDescriptor;

@FunctionalInterface
public interface PageResolver {
    JNode resolve(PageDescriptor descriptor);
}
