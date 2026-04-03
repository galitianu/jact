package io.jact.spring.boot.autoconfigure;

import io.jact.annotations.JNode;
import io.jact.core.api.Navigator;
import io.jact.core.internal.JactRuntimeException;
import io.jact.core.registry.GeneratedRuntimeRegistry;
import io.jact.core.registry.RuntimeRegistry;
import io.jact.core.routing.RouteParams;
import io.jact.core.runtime.JactRuntime;
import io.jact.core.runtime.RenderRequest;
import io.jact.core.runtime.WindowSettings;
import io.jact.core.runtime.spi.PageResolver;
import io.jact.core.runtime.spi.RendererBridge;
import io.jact.javafx.renderer.JavaFxRendererBridge;
import io.jact.spring.boot.properties.JactProperties;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

@AutoConfiguration
@EnableConfigurationProperties(JactProperties.class)
public class JactAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public RuntimeRegistry jactRuntimeRegistry(ApplicationContext applicationContext) {
        return GeneratedRuntimeRegistry.from(applicationContext.getClassLoader());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "jact", name = "enabled", havingValue = "true")
    public RendererBridge jactRendererBridge() {
        return new JavaFxRendererBridge();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "jact", name = "enabled", havingValue = "true")
    public JactRuntime jactRuntime(RuntimeRegistry runtimeRegistry, RendererBridge rendererBridge) {
        return new JactRuntime(runtimeRegistry, rendererBridge);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "jact", name = "enabled", havingValue = "true")
    public Navigator jactNavigator(JactRuntime runtime) {
        return runtime.navigator();
    }

    @Bean
    @ConditionalOnProperty(prefix = "jact", name = "enabled", havingValue = "true")
    public ApplicationRunner jactStartupRunner(
        JactRuntime runtime,
        JactProperties properties,
        ApplicationContext applicationContext
    ) {
        return args -> {
            runtime.start();
            PageResolver pageResolver = request -> invokePage(applicationContext, request);
            runtime.mountInitialPage(
                properties.getInitialPage(),
                pageResolver,
                new WindowSettings(properties.getWindowTitle(), properties.getWindowWidth(), properties.getWindowHeight())
            );
        };
    }

    private JNode invokePage(ApplicationContext applicationContext, RenderRequest request) {
        try {
            Class<?> beanClass = Class.forName(request.descriptor().beanClassName());
            Object bean = applicationContext.getBean(beanClass);
            Method method = resolvePageMethod(beanClass, request.descriptor().methodName());
            Object[] args = resolveMethodArguments(method, request);
            Object result = method.invoke(bean, args);

            if (!(result instanceof JNode node)) {
                throw new JactRuntimeException(
                    "Page method did not return JNode: " + request.descriptor().beanClassName() + "#" + request.descriptor().methodName()
                );
            }
            return node;
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException exception) {
            throw new JactRuntimeException(
                "Failed to invoke page: " + request.descriptor().beanClassName() + "#" + request.descriptor().methodName(),
                exception
            );
        }
    }

    private Method resolvePageMethod(Class<?> beanClass, String methodName) {
        return Arrays.stream(beanClass.getMethods())
            .filter(method -> method.getName().equals(methodName))
            .findFirst()
            .orElseThrow(() -> new JactRuntimeException("No matching page method found: " + beanClass.getName() + "#" + methodName));
    }

    private Object[] resolveMethodArguments(Method method, RenderRequest request) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] args = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (RouteParams.class.equals(parameterType)) {
                args[i] = request.params();
            } else if (Navigator.class.equals(parameterType)) {
                args[i] = request.navigator();
            } else {
                throw new JactRuntimeException(
                    "Unsupported page method parameter type: " + parameterType.getName() + " in " + method.getDeclaringClass().getName() + "#" + method.getName()
                );
            }
        }

        return args;
    }
}
