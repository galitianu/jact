package io.jact.sample;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(properties = "jact.enabled=false")
class TaskManagerApplicationTest {
    @Test
    void contextLoads(ApplicationContext applicationContext) {
        assertThat(applicationContext).isNotNull();
    }
}
