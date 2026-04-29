package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilNamingStyleTest {

    @Test
    void testNamingStyle() {
        assertEquals("helloWorld", StringUtil.toCamelCase("hello_world"));
        assertEquals("HelloWorld", StringUtil.toPascalCase("hello-world"));
        assertEquals("hello_world", StringUtil.toSnakeCase("helloWorld"));
        assertEquals("hello-world", StringUtil.toKebabCase("helloWorld"));
        assertEquals("hello_world", StringUtil.camelToSnake("helloWorld"));
        assertEquals("helloWorld", StringUtil.snakeToCamel("hello_world"));
        assertEquals("hello-world", StringUtil.camelToKebab("helloWorld"));
        assertEquals("helloWorld", StringUtil.kebabToCamel("hello-world"));
    }

}
