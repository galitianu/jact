package io.jact.core;

import io.jact.core.descriptor.ComponentDescriptor;
import io.jact.core.descriptor.PageDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GeneratedRuntimeRegistry implements RuntimeRegistry {
    public static final String COMPONENT_REGISTRY_CLASS = "io.jact.generated.GeneratedComponentRegistry";
    public static final String PAGE_REGISTRY_CLASS = "io.jact.generated.GeneratedPageRegistry";

    private final List<ComponentDescriptor> components;
    private final List<PageDescriptor> pages;

    private GeneratedRuntimeRegistry(List<ComponentDescriptor> components, List<PageDescriptor> pages) {
        this.components = List.copyOf(components);
        this.pages = List.copyOf(pages);
    }

    public static GeneratedRuntimeRegistry from(ClassLoader classLoader) {
        return new GeneratedRuntimeRegistry(loadComponents(classLoader), loadPages(classLoader));
    }

    @Override
    public List<ComponentDescriptor> components() {
        return components;
    }

    @Override
    public List<PageDescriptor> pages() {
        return pages;
    }

    private static List<ComponentDescriptor> loadComponents(ClassLoader classLoader) {
        String[][] entries = loadEntries(classLoader, COMPONENT_REGISTRY_CLASS);
        if (entries == null) {
            return Collections.emptyList();
        }

        List<ComponentDescriptor> descriptors = new ArrayList<>();
        for (String[] entry : entries) {
            if (entry.length < 2) {
                continue;
            }
            descriptors.add(new ComponentDescriptor(entry[0], entry[1]));
        }
        return descriptors;
    }

    private static List<PageDescriptor> loadPages(ClassLoader classLoader) {
        String[][] entries = loadEntries(classLoader, PAGE_REGISTRY_CLASS);
        if (entries == null) {
            return Collections.emptyList();
        }

        List<PageDescriptor> descriptors = new ArrayList<>();
        for (String[] entry : entries) {
            if (entry.length < 3) {
                continue;
            }
            descriptors.add(new PageDescriptor(entry[0], entry[1], entry[2]));
        }
        return descriptors;
    }

    private static String[][] loadEntries(ClassLoader classLoader, String className) {
        Class<?> registryClass;
        try {
            registryClass = Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException ignored) {
            return null;
        }

        Method entriesMethod;
        try {
            entriesMethod = registryClass.getMethod("entries");
        } catch (NoSuchMethodException exception) {
            throw new JactRuntimeException("Missing entries() on generated registry: " + className, exception);
        }

        try {
            Object rawResult = entriesMethod.invoke(null);
            if (!(rawResult instanceof String[][] entries)) {
                throw new JactRuntimeException("Generated registry did not return String[][]: " + className);
            }
            return entries;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new JactRuntimeException("Could not read generated registry: " + className, exception);
        }
    }
}
