package io.jact.compiler.processor;

import io.jact.annotations.JactComponent;
import io.jact.annotations.JactPage;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class JactAnnotationProcessor extends AbstractProcessor {
    private final List<ComponentEntry> componentEntries = new ArrayList<>();
    private final List<PageEntry> pageEntries = new ArrayList<>();
    private final Map<String, Element> routeSignatures = new HashMap<>();
    private boolean generated;

    private Messager messager;
    private Elements elements;
    private Types types;
    private Filer filer;
    private TypeMirror jNodeType;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
        filer = processingEnv.getFiler();

        TypeElement jNodeElement = elements.getTypeElement("io.jact.annotations.JNode");
        if (jNodeElement != null) {
            jNodeType = jNodeElement.asType();
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(
            JactComponent.class.getCanonicalName(),
            JactPage.class.getCanonicalName()
        );
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            if (!generated) {
                generated = true;
                writeComponentRegistry();
                writePageRegistry();
            }
            return false;
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(JactComponent.class)) {
            collectComponent(element);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(JactPage.class)) {
            collectPage(element);
        }

        return false;
    }

    private void collectComponent(Element element) {
        if (!(element instanceof ExecutableElement executableElement)) {
            error(element, "@JactComponent can only be used on methods.");
            return;
        }

        if (!validateCommonMethodRules(executableElement, "@JactComponent")) {
            return;
        }

        String owner = ((TypeElement) executableElement.getEnclosingElement()).getQualifiedName().toString();
        componentEntries.add(new ComponentEntry(owner, executableElement.getSimpleName().toString()));
    }

    private void collectPage(Element element) {
        if (!(element instanceof ExecutableElement executableElement)) {
            error(element, "@JactPage can only be used on methods.");
            return;
        }

        if (!validateCommonMethodRules(executableElement, "@JactPage")) {
            return;
        }

        JactPage annotation = executableElement.getAnnotation(JactPage.class);
        String derivedRoute = annotation.path().isBlank() ? deriveRoute(executableElement) : annotation.path();
        if (derivedRoute == null) {
            return;
        }

        String route = normalizePath(derivedRoute);
        String signature = routeSignature(route);
        Element previousElement = routeSignatures.putIfAbsent(signature, executableElement);
        if (previousElement != null) {
            error(executableElement, "Duplicate page route pattern '%s'.", route);
            error(previousElement, "Conflicting route pattern already declared here.");
            return;
        }

        String owner = ((TypeElement) executableElement.getEnclosingElement()).getQualifiedName().toString();
        pageEntries.add(new PageEntry(route, owner, executableElement.getSimpleName().toString()));
    }

    private boolean validateCommonMethodRules(ExecutableElement executableElement, String annotationName) {
        if (executableElement.getKind() != ElementKind.METHOD) {
            error(executableElement, "%s can only be used on methods.", annotationName);
            return false;
        }

        if (!executableElement.getModifiers().contains(Modifier.PUBLIC)) {
            error(executableElement, "%s methods must be public.", annotationName);
            return false;
        }

        if (jNodeType == null) {
            error(executableElement, "Could not resolve io.jact.annotations.JNode type.");
            return false;
        }

        if (!types.isAssignable(types.erasure(executableElement.getReturnType()), types.erasure(jNodeType))) {
            error(executableElement, "%s methods must return io.jact.annotations.JNode.", annotationName);
            return false;
        }

        return true;
    }

    private String deriveRoute(ExecutableElement executableElement) {
        TypeElement owner = (TypeElement) executableElement.getEnclosingElement();
        List<String> routeSegments = new ArrayList<>();

        String packageName = elements.getPackageOf(owner).getQualifiedName().toString();
        List<String> packageSegments = packageName.isBlank()
            ? List.of()
            : List.of(packageName.split("\\."));

        int startIndex = packageSegments.indexOf("pages");
        if (startIndex >= 0) {
            startIndex += 1;
            for (int i = startIndex; i < packageSegments.size(); i++) {
                String mapped = mapSegment(packageSegments.get(i), executableElement);
                if (mapped == null) {
                    return null;
                }
                if (!mapped.isBlank() && !"index".equals(mapped)) {
                    routeSegments.add(mapped);
                }
            }
        }

        String classSegment = owner.getSimpleName().toString()
            .replaceAll("Pages?$", "")
            .replaceAll("Page$", "");

        if (!classSegment.isBlank()) {
            String mappedClass = mapSegment(classSegment, executableElement);
            if (mappedClass == null) {
                return null;
            }
            if (!mappedClass.isBlank() && !"index".equals(mappedClass)) {
                routeSegments.add(mappedClass);
            }
        }

        if (routeSegments.isEmpty()) {
            return "/";
        }

        return "/" + String.join("/", routeSegments);
    }

    private String mapSegment(String rawSegment, Element errorElement) {
        if (rawSegment == null || rawSegment.isBlank()) {
            return "";
        }

        if (rawSegment.startsWith("$")) {
            if (!rawSegment.matches("\\$[A-Za-z][A-Za-z0-9_]*")) {
                error(errorElement, "Invalid dynamic route segment '%s'. Use $name with alphanumeric/underscore characters.", rawSegment);
                return null;
            }
            return rawSegment;
        }

        if (rawSegment.contains("$")) {
            error(errorElement, "Invalid segment '%s'. Dynamic route segments must start with '$'.", rawSegment);
            return null;
        }

        return rawSegment
            .replaceAll("([a-z])([A-Z])", "$1-$2")
            .toLowerCase(Locale.ROOT);
    }

    private String routeSignature(String route) {
        if ("/".equals(route)) {
            return route;
        }

        String[] segments = route.substring(1).split("/");
        List<String> normalized = new ArrayList<>(segments.length);
        for (String segment : segments) {
            normalized.add(segment.startsWith("$") ? "$" : segment);
        }
        return "/" + String.join("/", normalized);
    }

    private String normalizePath(String rawPath) {
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

    private void writeComponentRegistry() {
        List<ComponentEntry> sorted = componentEntries.stream()
            .sorted(Comparator.comparing(ComponentEntry::ownerClass).thenComparing(ComponentEntry::methodName))
            .toList();

        StringBuilder source = new StringBuilder();
        source.append("package io.jact.generated;\n\n");
        source.append("public final class GeneratedComponentRegistry {\n");
        source.append("  private GeneratedComponentRegistry() {}\n\n");
        source.append("  public static String[][] entries() {\n");
        source.append("    return new String[][] {\n");

        for (int i = 0; i < sorted.size(); i++) {
            ComponentEntry entry = sorted.get(i);
            source.append("      {\"")
                .append(escape(entry.ownerClass()))
                .append("\", \"")
                .append(escape(entry.methodName()))
                .append("\"}");
            if (i < sorted.size() - 1) {
                source.append(",");
            }
            source.append("\n");
        }

        source.append("    };\n");
        source.append("  }\n");
        source.append("}\n");

        writeSourceFile("io.jact.generated.GeneratedComponentRegistry", source.toString());
    }

    private void writePageRegistry() {
        List<PageEntry> sorted = pageEntries.stream()
            .sorted(Comparator.comparing(PageEntry::route).thenComparing(PageEntry::ownerClass).thenComparing(PageEntry::methodName))
            .toList();

        StringBuilder source = new StringBuilder();
        source.append("package io.jact.generated;\n\n");
        source.append("public final class GeneratedPageRegistry {\n");
        source.append("  private GeneratedPageRegistry() {}\n\n");
        source.append("  public static String[][] entries() {\n");
        source.append("    return new String[][] {\n");

        for (int i = 0; i < sorted.size(); i++) {
            PageEntry entry = sorted.get(i);
            source.append("      {\"")
                .append(escape(entry.route()))
                .append("\", \"")
                .append(escape(entry.ownerClass()))
                .append("\", \"")
                .append(escape(entry.methodName()))
                .append("\"}");
            if (i < sorted.size() - 1) {
                source.append(",");
            }
            source.append("\n");
        }

        source.append("    };\n");
        source.append("  }\n");
        source.append("}\n");

        writeSourceFile("io.jact.generated.GeneratedPageRegistry", source.toString());
    }

    private void writeSourceFile(String className, String content) {
        try {
            JavaFileObject fileObject = filer.createSourceFile(className);
            try (Writer writer = fileObject.openWriter()) {
                writer.write(content);
            }
        } catch (IOException exception) {
            error(null, "Failed to write %s: %s", className, exception.getMessage());
        }
    }

    private String escape(String rawValue) {
        return rawValue.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void error(Element element, String message, Object... args) {
        String formatted = String.format(message, args);
        if (element == null) {
            messager.printMessage(Diagnostic.Kind.ERROR, formatted);
            return;
        }
        messager.printMessage(Diagnostic.Kind.ERROR, formatted, element);
    }

    private record ComponentEntry(String ownerClass, String methodName) {
    }

    private record PageEntry(String route, String ownerClass, String methodName) {
    }
}
