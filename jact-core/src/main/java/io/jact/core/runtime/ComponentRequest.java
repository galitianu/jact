package io.jact.core.runtime;

import io.jact.core.api.Navigator;
import io.jact.core.meta.ComponentDescriptor;
import io.jact.core.routing.RouteParams;

import java.util.List;

public record ComponentRequest(
    String componentId,
    ComponentDescriptor descriptor,
    List<Object> arguments,
    String path,
    RouteParams params,
    Navigator navigator
) {
    public ComponentRequest {
        arguments = List.copyOf(arguments);
    }
}
