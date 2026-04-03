package io.jact.core.registry;

import io.jact.core.meta.ComponentDescriptor;
import io.jact.core.meta.PageDescriptor;

import java.util.List;

public interface RuntimeRegistry {
    List<ComponentDescriptor> components();

    List<PageDescriptor> pages();
}
