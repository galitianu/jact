package io.jact.spring.boot;

import io.jact.annotations.JNode;
import io.jact.core.*;
import io.jact.core.descriptor.PageDescriptor;
import io.jact.javafx.JavaFxRendererBridge;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
    @ConditionalOnProperty(prefix = "jact", name = "enabled", havingValue = "true")
    public ApplicationRunner jactStartupRunner(
        JactRuntime runtime,
        JactProperties properties,
        ApplicationContext applicationContext
    ) {
        return args -> {
            runtime.start();
            PageResolver pageResolver = descriptor -> invokePage(applicationContext, descriptor);
            runtime.mountInitialPage(
                properties.getInitialPage(),
                pageResolver,
                new WindowSettings(properties.getWindowTitle(), properties.getWindowWidth(), properties.getWindowHeight())
            );
        };
    }

    private JNode invokePage(ApplicationContext applicationContext, PageDescriptor descriptor) {
        try {
            Class<?> beanClass = Class.forName(descriptor.beanClassName());
            Object bean = applicationContext.getBean(beanClass);
            Method method = beanClass.getMethod(descriptor.methodName());
            Object result = method.invoke(bean);

            if (!(result instanceof JNode node)) {
                throw new JactRuntimeException("Page method did not return JNode: " + descriptor.beanClassName() + "#" + descriptor.methodName());
            }
            return node;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new JactRuntimeException("Failed to invoke page: " + descriptor.beanClassName() + "#" + descriptor.methodName(), exception);
        }
    }
}
