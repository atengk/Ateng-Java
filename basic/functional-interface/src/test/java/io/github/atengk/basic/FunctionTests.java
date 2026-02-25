package io.github.atengk.basic;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FunctionTests {

    /**
     * 1. 基础使用
     */
    @Test
    void testBasicFunction() {

        Function<String, Integer> lengthFunction =
                s -> s.length();

        Integer result = lengthFunction.apply("Java");

        System.out.println("字符串长度: " + result);
    }

    /**
     * 2. 类型转换
     */
    @Test
    void testTypeConversion() {

        Function<String, Integer> parseFunction =
                Integer::parseInt;

        Integer number = parseFunction.apply("100");

        System.out.println("转换结果: " + number);
    }

    /**
     * 3. Stream 映射
     *
     * map 本质接收 Function
     */
    @Test
    void testStreamMap() {

        List<String> list = Arrays.asList("java", "spring", "boot");

        List<Integer> lengths = list.stream()
                .map(String::length)
                .collect(Collectors.toList());

        System.out.println("长度集合: " + lengths);
    }

    /**
     * 4. 函数组合
     *
     * andThen：先执行当前函数，再执行后续函数
     * compose：先执行传入函数
     */
    @Test
    void testFunctionCompose() {

        Function<String, String> addPrefix =
                s -> "Hello " + s;

        Function<String, String> toUpper =
                String::toUpperCase;

        Function<String, String> combined =
                addPrefix.andThen(toUpper);

        System.out.println(combined.apply("java"));
    }

    /**
     * 5. DTO 转换示例
     */
    @Test
    void testDtoMapping() {

        Function<UserEntity, UserDTO> mapper = entity -> {
            UserDTO dto = new UserDTO();
            dto.setUsername(entity.getUsername());
            return dto;
        };

        UserEntity entity = new UserEntity();
        entity.setUsername("admin");

        UserDTO dto = mapper.apply(entity);

        System.out.println("DTO用户名: " + dto.getUsername());
    }

    static class UserEntity {
        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    static class UserDTO {
        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

}