package com.elmlite.platform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ElmLiteApplication.class)
class ElmLiteApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void applicationContextStarts() {
        assertThat(applicationContext).isNotNull();
    }
}
