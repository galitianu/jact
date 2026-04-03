package io.jact.core.routing;

import io.jact.core.internal.JactRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RouteTemplate {
    private final String template;

    private RouteTemplate(String template) {
        this.template = normalizePath(template);
    }

    public static RouteTemplate of(String template) {
        return new RouteTemplate(template);
    }

    public String value() {
        return template;
    }

    public String resolve(RouteParams params) {
        Objects.requireNonNull(params, "params");

        if ("/".equals(template)) {
            return "/";
        }

        String[] parts = template.substring(1).split("/");
        List<String> resolved = new ArrayList<>(parts.length);

        for (String part : parts) {
            if (part.startsWith("$") && part.length() > 1) {
                String key = part.substring(1);
                String value = params.get(key);
                if (value == null || value.isBlank()) {
                    throw new JactRuntimeException("Missing route param: " + key + " for template: " + template);
                }
                resolved.add(value);
            } else {
                resolved.add(part);
            }
        }

        return "/" + String.join("/", resolved);
    }

    public static String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank() || "/".equals(rawPath)) {
            return "/";
        }

        String normalized = rawPath.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }
}
