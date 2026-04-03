package io.jact.core.runtime;

import io.jact.core.api.Navigator;
import io.jact.core.meta.PageDescriptor;
import io.jact.core.routing.RouteParams;

public record RenderRequest(
    String path,
    PageDescriptor descriptor,
    RouteParams params,
    Navigator navigator
) {
}
