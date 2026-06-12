package io.jact.core.node;

import java.util.List;

public record NodeStyle(
    List<String> styleClasses,
    String inlineStyle,
    Double minWidth,
    Double prefWidth,
    Double maxWidth,
    Double minHeight,
    Double prefHeight,
    Double maxHeight,
    Double spacing,
    String alignment,
    Boolean disabled
) {
    public NodeStyle {
        styleClasses = List.copyOf(styleClasses);
        inlineStyle = inlineStyle == null ? "" : inlineStyle;
    }

    public static NodeStyle empty() {
        return new NodeStyle(List.of(), "", null, null, null, null, null, null, null, null, null);
    }

    public static NodeStyle className(String className) {
        return empty().withClassName(className);
    }

    public NodeStyle withClassName(String className) {
        if (className == null || className.isBlank()) {
            return this;
        }
        java.util.ArrayList<String> nextClasses = new java.util.ArrayList<>(styleClasses);
        nextClasses.add(className);
        return new NodeStyle(
            nextClasses,
            inlineStyle,
            minWidth,
            prefWidth,
            maxWidth,
            minHeight,
            prefHeight,
            maxHeight,
            spacing,
            alignment,
            disabled
        );
    }

    public NodeStyle withInlineStyle(String nextInlineStyle) {
        return new NodeStyle(styleClasses, nextInlineStyle, minWidth, prefWidth, maxWidth, minHeight, prefHeight, maxHeight, spacing, alignment, disabled);
    }

    public NodeStyle withPrefWidth(double nextPrefWidth) {
        return new NodeStyle(styleClasses, inlineStyle, minWidth, nextPrefWidth, maxWidth, minHeight, prefHeight, maxHeight, spacing, alignment, disabled);
    }

    public NodeStyle withPrefHeight(double nextPrefHeight) {
        return new NodeStyle(styleClasses, inlineStyle, minWidth, prefWidth, maxWidth, minHeight, nextPrefHeight, maxHeight, spacing, alignment, disabled);
    }

    public NodeStyle withSpacing(double nextSpacing) {
        return new NodeStyle(styleClasses, inlineStyle, minWidth, prefWidth, maxWidth, minHeight, prefHeight, maxHeight, nextSpacing, alignment, disabled);
    }

    public NodeStyle withAlignment(String nextAlignment) {
        return new NodeStyle(styleClasses, inlineStyle, minWidth, prefWidth, maxWidth, minHeight, prefHeight, maxHeight, spacing, nextAlignment, disabled);
    }

    public NodeStyle withDisabled(boolean nextDisabled) {
        return new NodeStyle(styleClasses, inlineStyle, minWidth, prefWidth, maxWidth, minHeight, prefHeight, maxHeight, spacing, alignment, nextDisabled);
    }

    public NodeStyle merge(NodeStyle childStyle) {
        if (childStyle == null) {
            return this;
        }
        return new NodeStyle(
            mergeClasses(styleClasses, childStyle.styleClasses),
            childStyle.inlineStyle.isBlank() ? inlineStyle : childStyle.inlineStyle,
            childStyle.minWidth == null ? minWidth : childStyle.minWidth,
            childStyle.prefWidth == null ? prefWidth : childStyle.prefWidth,
            childStyle.maxWidth == null ? maxWidth : childStyle.maxWidth,
            childStyle.minHeight == null ? minHeight : childStyle.minHeight,
            childStyle.prefHeight == null ? prefHeight : childStyle.prefHeight,
            childStyle.maxHeight == null ? maxHeight : childStyle.maxHeight,
            childStyle.spacing == null ? spacing : childStyle.spacing,
            childStyle.alignment == null ? alignment : childStyle.alignment,
            childStyle.disabled == null ? disabled : childStyle.disabled
        );
    }

    private static List<String> mergeClasses(List<String> first, List<String> second) {
        java.util.ArrayList<String> merged = new java.util.ArrayList<>(first);
        merged.addAll(second);
        return List.copyOf(merged);
    }
}
