package io.github.atengk.bean;

import io.github.atengk.bean.fixture.UserEntity;
import io.github.atengk.utils.BeanUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BeanUtil Bean 比较和 Bean 校验辅助单元测试。
 *
 * @author Ateng
 * @since 2026-04-29
 */
@DisplayName("BeanUtil Bean 比较和 Bean 校验辅助")
class BeanUtilCompareAndValidationTest {

    @Test
    @DisplayName("比较 Bean 属性差异")
    void shouldCompareBeanProperties() {
        UserEntity source = new UserEntity(1001L, "before");
        source.setAge(20);
        UserEntity target = new UserEntity(1001L, "after");
        target.setAge(21);

        List<BeanUtil.BeanDifference> differences = BeanUtil.compareProperties(source, target);

        assertTrue(differences.stream().anyMatch(diff -> "username".equals(diff.propertyName())));
        assertTrue(differences.stream().anyMatch(diff -> "age".equals(diff.propertyName())));
        assertTrue(BeanUtil.hasChanges(source, target));
        assertFalse(BeanUtil.equalsProperties(source, target));
    }

    @Test
    @DisplayName("比较指定属性")
    void shouldCompareSingleProperty() {
        UserEntity source = new UserEntity(1002L, "before");
        UserEntity target = new UserEntity(1002L, "after");

        BeanUtil.BeanDifference difference = BeanUtil.compareProperty(source, target, "username").orElseThrow();

        assertEquals("username", difference.propertyName());
        assertEquals("before", difference.sourceValue());
        assertEquals("after", difference.targetValue());
        assertEquals(BeanUtil.DifferenceType.VALUE_DIFFERENT, difference.type());
    }

    @Test
    @DisplayName("获取变化属性名和差异映射")
    void shouldGetChangedPropertyNamesAndMap() {
        UserEntity source = new UserEntity(1003L, "source");
        UserEntity target = new UserEntity(1004L, "target");

        List<String> changedNames = BeanUtil.getChangedPropertyNames(source, target);
        Map<String, BeanUtil.BeanDifference> changedMap = BeanUtil.getChangedPropertyMap(source, target);

        assertTrue(changedNames.contains("id"));
        assertTrue(changedNames.contains("username"));
        assertTrue(changedMap.containsKey("id"));
    }

    @Test
    @DisplayName("比较数组值")
    void shouldCompareArrayValues() {
        assertTrue(BeanUtil.isSameValue(new int[]{1, 2}, new int[]{1, 2}));
        assertFalse(BeanUtil.isSameValue(new int[]{1, 2}, new int[]{1, 3}));
    }

    @Test
    @DisplayName("校验 Bean、字段和属性")
    void shouldRequireBeanFieldAndProperty() {
        assertDoesNotThrow(() -> BeanUtil.requireBean(new UserEntity()));
        assertDoesNotThrow(() -> BeanUtil.requireProperty(UserEntity.class, "username"));
        assertDoesNotThrow(() -> BeanUtil.requireReadableProperty(UserEntity.class, "username"));
        assertDoesNotThrow(() -> BeanUtil.requireWritableProperty(UserEntity.class, "username"));
        assertDoesNotThrow(() -> BeanUtil.requireFieldExists(UserEntity.class, "id"));
        assertThrows(BeanUtil.BeanException.class, () -> BeanUtil.requireProperty(UserEntity.class, "notExists"));
    }

    @Test
    @DisplayName("校验属性值非空")
    void shouldRequireNotEmptyProperties() {
        UserEntity user = new UserEntity(1005L, "ateng");

        assertDoesNotThrow(() -> BeanUtil.requireNotNullProperty(user, "id"));
        assertDoesNotThrow(() -> BeanUtil.requireNotEmptyProperty(user, "username"));
        assertDoesNotThrow(() -> BeanUtil.requireNotBlankProperty(user, "username"));

        user.setUsername("   ");
        assertThrows(BeanUtil.BeanException.class, () -> BeanUtil.requireNotBlankProperty(user, "username"));
    }

    @Test
    @DisplayName("获取指定范围的空属性名")
    void shouldGetBlankPropertyNamesByScope() {
        UserEntity user = new UserEntity();
        user.setUsername("   ");

        assertTrue(BeanUtil.getNullPropertyNames(user, List.of("id", "age")).containsAll(List.of("id", "age")));
        assertTrue(BeanUtil.getEmptyPropertyNames(user, List.of("id", "username")).contains("id"));
        assertTrue(BeanUtil.getBlankPropertyNames(user, List.of("username")).contains("username"));
    }

    @Test
    @DisplayName("校验必填属性")
    void shouldValidateRequiredProperties() {
        UserEntity user = new UserEntity();
        user.setUsername("   ");

        BeanUtil.BeanValidationResult notNullResult = BeanUtil.validateRequiredProperties(user, List.of("id"));
        BeanUtil.BeanValidationResult notBlankResult = BeanUtil.validateNotBlankProperties(user, List.of("username"));

        assertFalse(notNullResult.valid());
        assertFalse(notBlankResult.valid());
        assertEquals(BeanUtil.ViolationType.NULL_VALUE, notNullResult.violations().getFirst().type());
        assertEquals(BeanUtil.ViolationType.BLANK_VALUE, notBlankResult.violations().getFirst().type());
    }

    @Test
    @DisplayName("校验 Bean 并断言结果")
    void shouldValidateBeanWithOptions() {
        UserEntity user = new UserEntity(1006L, "validator");
        BeanUtil.BeanValidationOptions options = new BeanUtil.BeanValidationOptions(
                List.of("id"),
                List.of("username"),
                List.of("username"),
                true
        );

        BeanUtil.BeanValidationResult result = BeanUtil.validateBean(user, options);

        assertTrue(result.valid());
        assertTrue(result.messages().isEmpty());
        assertTrue(BeanUtil.isValidBean(user, options));
        assertDoesNotThrow(() -> BeanUtil.assertValidBean(user, options));
    }

    @Test
    @DisplayName("Bean 为空时返回校验失败结果")
    void shouldReturnInvalidResultWhenBeanIsNull() {
        BeanUtil.BeanValidationResult result = BeanUtil.validateBean(null, BeanUtil.BeanValidationOptions.defaults());

        assertFalse(result.valid());
        assertEquals(BeanUtil.ViolationType.BEAN_NULL, result.violations().getFirst().type());
        assertFalse(result.firstMessage().isBlank());
    }
}
