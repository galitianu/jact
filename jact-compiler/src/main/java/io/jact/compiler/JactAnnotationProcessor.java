package io.jact.compiler;

import io.jact.annotations.JactComponent;
import io.jact.annotations.JactPage;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class JactAnnotationProcessor extends AbstractProcessor {
    private final List<ComponentEntry> componentEntries = new ArrayList<>();
    private final List<PageEntry> pageEntries = new ArrayList<>();
    private final Map<String, Element> routes = new HashMap<>();
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
        String route = normalizePath(annotation.path().isBlank() ? deriveRoute(executableElement) : annotation.path());
        Element previousElement = routes.putIfAbsent(route, executableElement);
        if (previousElement != null) {
            error(executableElement, "Duplicate page route '%s'.", route);
            error(previousElement, "Route '%s' already declared here.", route);
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
        String className = owner.getSimpleName().toString();
        String routeBase = className
            .replaceAll("Pages?$", "")
            .replaceAll("Page$", "");

        if (routeBase.isBlank()) {
            return "/";
        }

        String kebab = routeBase
            .replaceAll("([a-z])([A-Z])", "$1-$2")
            .toLowerCase(Locale.ROOT);

        if ("index".equals(kebab)) {
            return "/";
        }

        return "/" + kebab;
    }

    private String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank() || "/".equals(rawPath)) {
            return "/";
        }
        return rawPath.startsWith("/") ? rawPath : "/" + rawPath;
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
