package io.github.atengk.bean;

import io.github.atengk.utils.BeanUtil;
import io.github.atengk.bean.fixture.PrimitiveBean;
import io.github.atengk.bean.fixture.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BeanUtil 属性元信息和属性判断单元测试。
 *
 * @author Ateng
 * @since 2026-04-29
 */
@DisplayName("BeanUtil 属性元信息和属性判断")
class BeanUtilMetadataAndPropertyCheckTest {

    @Test
    @DisplayName("获取字段元信息")
    void shouldGetFieldMetadata() {
        List<Field> fields = BeanUtil.getFields(UserEntity.class);
        Map<String, Field> fieldMap = BeanUtil.getFieldMap(UserEntity.class);

        assertTrue(fields.stream().anyMatch(field -> "username".equals(field.getName())));
        assertTrue(fieldMap.containsKey("id"));
        assertEquals(String.class, BeanUtil.getFieldType(UserEntity.class, "username"));
        assertTrue(BeanUtil.findField(UserEntity.class, "age").isPresent());
    }

    @Test
    @DisplayName("获取属性描述和属性类型")
    void shouldGetPropertyDescriptorsAndTypes() {
        List<PropertyDescriptor> descriptors = BeanUtil.getPropertyDescriptors(UserEntity.class);
        Map<String, Class<?>> propertyTypes = BeanUtil.getPropertyTypes(UserEntity.class);

        assertTrue(descriptors.stream().anyMatch(descriptor -> "username".equals(descriptor.getName())));
        assertEquals(Long.class, propertyTypes.get("id"));
        assertEquals(Integer.class, BeanUtil.getPropertyType(UserEntity.class, "age"));
    }

    @Test
    @DisplayName("获取 Bean 属性元信息")
    void shouldGetBeanPropertyMetadata() {
        BeanUtil.BeanProperty property = BeanUtil.getBeanProperty(UserEntity.class, "username");

        assertEquals("username", property.name());
        assertEquals(String.class, property.type());
        assertTrue(property.readable());
        assertTrue(property.writable());
    }

    @Test
    @DisplayName("获取可读和可写属性名")
    void shouldGetReadableAndWritablePropertyNames() {
        List<String> readableNames = BeanUtil.getReadablePropertyNames(UserEntity.class);
        List<String> writableNames = BeanUtil.getWritablePropertyNames(UserEntity.class);

        assertTrue(readableNames.contains("username"));
        assertTrue(writableNames.contains("username"));
        assertFalse(writableNames.contains("immutableCode"));
    }

    @Test
    @DisplayName("判断属性和字段是否存在且可访问")
    void shouldCheckPropertyAndFieldState() {
        assertTrue(BeanUtil.hasField(UserEntity.class, "id"));
        assertTrue(BeanUtil.hasProperty(UserEntity.class, "username"));
        assertTrue(BeanUtil.isReadableProperty(UserEntity.class, "username"));
        assertTrue(BeanUtil.isWritableProperty(UserEntity.class, "username"));
        assertFalse(BeanUtil.isWritableProperty(UserEntity.class, "immutableCode"));
    }

    @Test
    @DisplayName("判断 Getter 和 Setter")
    void shouldCheckGetterAndSetter() throws NoSuchMethodException {
        Method getter = UserEntity.class.getDeclaredMethod("getUsername");
        Method setter = UserEntity.class.getDeclaredMethod("setUsername", String.class);
        Method booleanGetter = PrimitiveBean.class.getDeclaredMethod("isActive");

        assertTrue(BeanUtil.isGetter(getter));
        assertTrue(BeanUtil.isGetter(booleanGetter));
        assertTrue(BeanUtil.isSetter(setter));
        assertEquals("username", BeanUtil.getPropertyName(getter));
    }

    @Test
    @DisplayName("判断简单值类型和 Bean 类型")
    void shouldCheckSimpleValueTypeAndBeanClass() {
        assertTrue(BeanUtil.isSimpleValueType(String.class));
        assertTrue(BeanUtil.isSimpleValueType(BigDecimal.class));
        assertTrue(BeanUtil.isSimpleValueType(LocalDateTime.class));
        assertFalse(BeanUtil.isBeanClass(String.class));
        assertTrue(BeanUtil.isBeanClass(UserEntity.class));
    }

    @Test
    @DisplayName("包装和拆箱基本类型")
    void shouldWrapAndUnwrapPrimitiveTypes() {
        assertEquals(Integer.class, BeanUtil.wrapPrimitive(int.class));
        assertEquals(int.class, BeanUtil.unwrapWrapper(Integer.class));
        assertTrue(BeanUtil.isAssignableValue(int.class, 1));
        assertFalse(BeanUtil.isAssignableValue(int.class, null));
    }
}
