package io.jact.spring.boot.autoconfigure;

import io.jact.annotations.JNode;
import io.jact.core.api.Navigator;
import io.jact.core.internal.JactRuntimeException;
import io.jact.core.runtime.ComponentRequest;
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
import org.springframework.beans.BeansException;
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
                request -> invokeComponent(applicationContext, request),
                new WindowSettings(properties.getWindowTitle(), properties.getWindowWidth(), properties.getWindowHeight())
            );
        };
    }

    private JNode invokePage(ApplicationContext applicationContext, RenderRequest request) {
        return invokeJactMethod(
            applicationContext,
            request.descriptor().beanClassName(),
            request.descriptor().methodName(),
            request.params(),
            request.navigator(),
            new Object[0],
            "page"
        );
    }

    private JNode invokeComponent(ApplicationContext applicationContext, ComponentRequest request) {
        return invokeJactMethod(
            applicationContext,
            request.descriptor().beanClassName(),
            request.descriptor().methodName(),
            request.params(),
            request.navigator(),
            request.arguments().toArray(),
            "component"
        );
    }

    private JNode invokeJactMethod(
        ApplicationContext applicationContext,
        String beanClassName,
        String methodName,
        RouteParams routeParams,
        Navigator navigator,
        Object[] explicitArgs,
        String kind
    ) {
        try {
            Class<?> beanClass = Class.forName(beanClassName);
            Object bean = applicationContext.getBean(beanClass);
            Method method = resolveMethod(beanClass, methodName);
            Object[] args = resolveMethodArguments(applicationContext, method, routeParams, navigator, explicitArgs);
            Object result = method.invoke(bean, args);

            if (!(result instanceof JNode node)) {
                throw new JactRuntimeException(
                    "JACT " + kind + " method did not return JNode: " + beanClassName + "#" + methodName
                );
            }
            return node;
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | BeansException exception) {
            throw new JactRuntimeException(
                "Failed to invoke JACT " + kind + ": " + beanClassName + "#" + methodName,
                exception
            );
        }
    }

    private Method resolveMethod(Class<?> beanClass, String methodName) {
        Method[] matches = Arrays.stream(beanClass.getMethods())
            .filter(method -> method.getName().equals(methodName))
            .toArray(Method[]::new);
        if (matches.length == 0) {
            throw new JactRuntimeException("No matching JACT method found: " + beanClass.getName() + "#" + methodName);
        }
        if (matches.length > 1) {
            throw new JactRuntimeException("Ambiguous JACT method overload: " + beanClass.getName() + "#" + methodName);
        }
        return matches[0];
    }

    private Object[] resolveMethodArguments(
        ApplicationContext applicationContext,
        Method method,
        RouteParams routeParams,
        Navigator navigator,
        Object[] explicitArgs
    ) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] args = new Object[parameterTypes.length];
        boolean[] usedExplicitArgs = new boolean[explicitArgs.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (RouteParams.class.equals(parameterType)) {
                args[i] = routeParams;
            } else if (Navigator.class.equals(parameterType)) {
                args[i] = navigator;
            } else {
                int explicitIndex = findExplicitArgument(parameterType, explicitArgs, usedExplicitArgs);
                if (explicitIndex >= 0) {
                    usedExplicitArgs[explicitIndex] = true;
                    args[i] = explicitArgs[explicitIndex];
                } else {
                    args[i] = applicationContext.getBean(parameterType);
                }
            }
        }

        return args;
    }

    private int findExplicitArgument(Class<?> parameterType, Object[] explicitArgs, boolean[] usedExplicitArgs) {
        for (int i = 0; i < explicitArgs.length; i++) {
            if (usedExplicitArgs[i]) {
                continue;
            }
            Object candidate = explicitArgs[i];
            if (candidate == null) {
                if (!parameterType.isPrimitive()) {
                    return i;
                }
            } else {
                Class<?> effectiveType = wrapPrimitive(parameterType);
                if (effectiveType.isInstance(candidate)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private Class<?> wrapPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }
}
