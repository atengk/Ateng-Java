package io.github.atengk.pool;

import io.github.atengk.pool.config.ThreadPoolProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AtengTests {
    @Autowired
    private ThreadPoolProperties properties;

    @Test
    public void test() {
        System.out.println(properties);
    }

}
