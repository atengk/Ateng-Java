package io.github.atengk.bean;

import io.github.atengk.utils.BeanUtil;
import io.github.atengk.bean.fixture.Address;
import io.github.atengk.bean.fixture.TestMarker;
import io.github.atengk.bean.fixture.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * BeanUtil 嵌套属性、字段过滤和注解辅助单元测试。
 *
 * @author Ateng
 * @since 2026-04-29
 */
@DisplayName("BeanUtil 嵌套属性、字段过滤和注解辅助")
class BeanUtilNestedFilterAnnotationTest {

    @Test
    @DisplayName("读取和写入嵌套属性")
    void shouldReadAndWriteNestedProperty() {
        UserEntity user = new UserEntity();
        user.setAddress(new Address("长沙", "五一大道"));

        assertEquals("长沙", BeanUtil.getNestedPropertyValue(user, "address.city"));

        BeanUtil.setNestedPropertyValue(user, "address.city", "杭州");

        assertEquals("杭州", user.getAddress().getCity());
    }

    @Test
    @DisplayName("自动创建中间对象并写入嵌套属性")
    void shouldAutoCreateNestedBeanWhenSetNestedProperty() {
        UserEntity user = new UserEntity();

        BeanUtil.setNestedPropertyValue(user, "address.city", "深圳");

        assertNotNull(user.getAddress());
        assertEquals("深圳", user.getAddress().getCity());
    }

    @Test
    @DisplayName("判断嵌套属性状态")
    void shouldCheckNestedPropertyState() {
        assertTrue(BeanUtil.hasNestedProperty(UserEntity.class, "address.city"));
        assertTrue(BeanUtil.isReadableNestedProperty(UserEntity.class, "address.city"));
        assertTrue(BeanUtil.isWritableNestedProperty(UserEntity.class, "address.city"));
        assertFalse(BeanUtil.hasNestedProperty(UserEntity.class, "address.notExists"));
    }

    @Test
    @DisplayName("解析属性路径")
    void shouldParsePropertyPath() {
        List<String> path = BeanUtil.parsePropertyPath("address.city");

        assertEquals(List.of("address", "city"), path);
        assertThrows(IllegalArgumentException.class, () -> BeanUtil.parsePropertyPath("address."));
    }

    @Test
    @DisplayName("根据字段过滤配置获取字段")
    void shouldFilterFields() {
        List<Field> fields = BeanUtil.getFields(UserEntity.class, new BeanUtil.FieldFilterOptions(
                List.of("id", "username", "immutableCode"),
                List.of("username"),
                false,
                true,
                null
        ));

        List<String> fieldNames = fields.stream().map(Field::getName).toList();
        assertTrue(fieldNames.contains("id"));
        assertFalse(fieldNames.contains("username"));
        assertFalse(fieldNames.contains("immutableCode"));
    }

    @Test
    @DisplayName("根据属性过滤配置获取属性")
    void shouldFilterProperties() {
        List<BeanUtil.BeanProperty> properties = BeanUtil.getBeanProperties(UserEntity.class, BeanUtil.PropertyFilterOptions.ofOnlyWritable());
        List<String> propertyNames = BeanUtil.getPropertyNames(UserEntity.class, List.of("id", "username", "immutableCode"), List.of("id"));

        assertTrue(properties.stream().anyMatch(property -> "username".equals(property.name())));
        assertFalse(properties.stream().anyMatch(property -> "immutableCode".equals(property.name())));
        assertEquals(2, propertyNames.size());
        assertTrue(propertyNames.contains("username"));
        assertTrue(propertyNames.contains("immutableCode"));
    }

    @Test
    @DisplayName("过滤 Map 属性")
    void shouldFilterPropertyMap() {
        Map<String, Object> source = Map.of("id", 1001L, "username", "ateng", "age", 20);

        Map<String, Object> result = BeanUtil.filterPropertyMap(source, List.of("id", "username"), List.of("id"));

        assertEquals(1, result.size());
        assertEquals("ateng", result.get("username"));
    }

    @Test
    @DisplayName("读取类、字段、方法和属性注解")
    void shouldReadAnnotations() throws NoSuchFieldException, NoSuchMethodException {
        Field idField = UserEntity.class.getDeclaredField("id");
        Method setUsernameMethod = UserEntity.class.getDeclaredMethod("setUsername", String.class);

        assertTrue(BeanUtil.hasAnnotation(UserEntity.class, TestMarker.class));
        assertTrue(BeanUtil.findAnnotation(idField, TestMarker.class).isPresent());
        assertTrue(BeanUtil.hasAnnotation(setUsernameMethod, TestMarker.class));
        assertTrue(BeanUtil.hasPropertyAnnotation(UserEntity.class, "id", TestMarker.class));
        assertTrue(BeanUtil.findPropertyAnnotation(UserEntity.class, "username", TestMarker.class).isPresent());
    }

    @Test
    @DisplayName("获取带注解成员")
    void shouldGetAnnotatedMembers() {
        List<Field> fields = BeanUtil.getAnnotatedFields(UserEntity.class, TestMarker.class);
        List<Method> methods = BeanUtil.getAnnotatedMethods(UserEntity.class, TestMarker.class);
        List<BeanUtil.BeanProperty> properties = BeanUtil.getAnnotatedProperties(UserEntity.class, TestMarker.class);
        List<Annotation> propertyAnnotations = BeanUtil.getPropertyAnnotations(UserEntity.class, "id");

        assertTrue(fields.stream().anyMatch(field -> "id".equals(field.getName())));
        assertTrue(methods.stream().anyMatch(method -> "setUsername".equals(method.getName())));
        assertTrue(properties.stream().anyMatch(property -> "id".equals(property.name())));
        assertFalse(propertyAnnotations.isEmpty());
    }
}
