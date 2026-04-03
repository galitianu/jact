package io.jact.compiler;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import io.jact.compiler.processor.JactAnnotationProcessor;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.JavaFileObjects.forSourceString;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JactAnnotationProcessorTest {
    @Test
    void generatesRegistriesForValidAnnotations() {
        JavaFileObject page = forSourceString("io.jact.sample.HomePages", """
            package io.jact.sample;

            import io.jact.annotations.JNode;
            import io.jact.annotations.JactPage;

            public class HomePages {
              @JactPage(path = "/")
              public JNode home() {
                return new JNode() {};
              }
            }
            """);

        JavaFileObject component = forSourceString("io.jact.sample.LayoutComponents", """
            package io.jact.sample;

            import io.jact.annotations.JNode;
            import io.jact.annotations.JactComponent;

            public class LayoutComponents {
              @JactComponent
              public JNode header() {
                return new JNode() {};
              }
            }
            """);

        Compilation compilation = Compiler.javac()
            .withProcessors(new JactAnnotationProcessor())
            .compile(page, component);

        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("io.jact.generated.GeneratedPageRegistry");
        assertThat(compilation).generatedSourceFile("io.jact.generated.GeneratedComponentRegistry");
    }

    @Test
    void failsWhenAnnotatedMethodReturnsWrongType() {
        JavaFileObject invalidPage = forSourceString("io.jact.sample.InvalidPages", """
            package io.jact.sample;

            import io.jact.annotations.JactPage;

            public class InvalidPages {
              @JactPage
              public String home() {
                return "nope";
              }
            }
            """);

        Compilation compilation = Compiler.javac()
            .withProcessors(new JactAnnotationProcessor())
            .compile(invalidPage);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("must return io.jact.annotations.JNode");
    }

    @Test
    void failsOnDuplicatePageRoutes() {
        JavaFileObject pageA = forSourceString("io.jact.sample.PageA", """
            package io.jact.sample;

            import io.jact.annotations.JNode;
            import io.jact.annotations.JactPage;

            public class PageA {
              @JactPage(path = "/same")
              public JNode first() {
                return new JNode() {};
              }
            }
            """);

        JavaFileObject pageB = forSourceString("io.jact.sample.PageB", """
            package io.jact.sample;

            import io.jact.annotations.JNode;
            import io.jact.annotations.JactPage;

            public class PageB {
              @JactPage(path = "/same")
              public JNode second() {
                return new JNode() {};
              }
            }
            """);

        Compilation compilation = Compiler.javac()
            .withProcessors(new JactAnnotationProcessor())
            .compile(pageA, pageB);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Duplicate page route pattern '/same'");
    }

    @Test
    void generatesDynamicRouteFromPagesPackageConvention() throws Exception {
        JavaFileObject dynamicPage = forSourceString("io.jact.sample.pages.tasks.$id.TaskDetailPage", """
            package io.jact.sample.pages.tasks.$id;

            import io.jact.annotations.JNode;
            import io.jact.annotations.JactPage;

            public class TaskDetailPage {
              @JactPage
              public JNode taskDetail() {
                return new JNode() {};
              }
            }
            """);

        Compilation compilation = Compiler.javac()
            .withProcessors(new JactAnnotationProcessor())
            .compile(dynamicPage);

        assertThat(compilation).succeeded();
        JavaFileObject generated = compilation.generatedSourceFile("io.jact.generated.GeneratedPageRegistry")
            .orElseThrow();
        assertTrue(generated.getCharContent(false).toString().contains("/tasks/$id/task-detail"));
    }
}
