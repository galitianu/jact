package io.jact.core;

import io.jact.core.descriptor.ComponentDescriptor;
import io.jact.core.descriptor.PageDescriptor;

import java.util.List;

public interface RuntimeRegistry {
    List<ComponentDescriptor> components();

    List<PageDescriptor> pages();
}
