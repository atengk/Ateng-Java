package io.github.atengk.bean;

import io.github.atengk.utils.BeanUtil;
import io.github.atengk.bean.fixture.LoginStatus;
import io.github.atengk.bean.fixture.UserDTO;
import io.github.atengk.bean.fixture.UserEntity;
import io.github.atengk.bean.fixture.UserStringSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BeanUtil Bean 拷贝、批量拷贝和属性类型转换单元测试。
 *
 * @author Ateng
 * @since 2026-04-29
 */
@DisplayName("BeanUtil Bean 拷贝、批量拷贝和属性类型转换")
class BeanUtilCopyAndConvertTest {

    @Test
    @DisplayName("拷贝 Bean 属性到已有对象")
    void shouldCopyPropertiesToExistingBean() {
        UserEntity source = new UserEntity(1001L, "ateng");
        source.setAge(25);
        source.setEnabled(true);
        UserDTO target = new UserDTO();

        BeanUtil.copyProperties(source, target);

        assertEquals(1001L, target.getId());
        assertEquals("ateng", target.getUsername());
        assertEquals(25, target.getAge());
        assertTrue(target.getEnabled());
    }

    @Test
    @DisplayName("拷贝 Bean 时忽略空值")
    void shouldCopyPropertiesIgnoringNull() {
        UserEntity source = new UserEntity();
        source.setUsername(null);
        source.setAge(30);
        UserDTO target = new UserDTO();
        target.setUsername("before");

        BeanUtil.copyProperties(source, target, true);

        assertEquals("before", target.getUsername());
        assertEquals(30, target.getAge());
    }

    @Test
    @DisplayName("拷贝 Bean 并创建目标对象")
    void shouldCopyToNewBean() {
        UserEntity source = new UserEntity(1002L, "copy-user");
        source.setStatus(LoginStatus.ENABLED);

        UserDTO target = BeanUtil.copyToBean(source, UserDTO.class);

        assertEquals(1002L, target.getId());
        assertEquals("copy-user", target.getUsername());
        assertEquals(LoginStatus.ENABLED, target.getStatus());
    }

    @Test
    @DisplayName("批量拷贝集合")
    void shouldCopyList() {
        List<UserEntity> sourceList = List.of(
                new UserEntity(1003L, "user-1"),
                new UserEntity(1004L, "user-2")
        );

        List<UserDTO> result = BeanUtil.copyList(sourceList, UserDTO.class);

        assertEquals(2, result.size());
        assertEquals("user-1", result.getFirst().getUsername());
        assertEquals("user-2", result.get(1).getUsername());
    }

    @Test
    @DisplayName("批量拷贝数组")
    void shouldCopyArray() {
        UserEntity[] sourceArray = {
                new UserEntity(1005L, "array-1"),
                new UserEntity(1006L, "array-2")
        };

        List<UserDTO> result = BeanUtil.copyArray(sourceArray, UserDTO.class);

        assertEquals(2, result.size());
        assertEquals(1005L, result.getFirst().getId());
    }

    @Test
    @DisplayName("批量填充目标集合")
    void shouldCopyListToTargetCollection() {
        List<UserEntity> sourceList = List.of(new UserEntity(1007L, "target-list"));
        List<UserDTO> targetList = new ArrayList<>();

        BeanUtil.copyListTo(sourceList, targetList, UserDTO.class);

        assertEquals(1, targetList.size());
        assertEquals("target-list", targetList.getFirst().getUsername());
    }

    @Test
    @DisplayName("Bean 拷贝时执行属性类型转换")
    void shouldConvertValueWhenCopyProperties() {
        UserStringSource source = new UserStringSource();
        source.setId("1008");
        source.setAge("27");
        source.setEnabled("true");
        source.setStatus("ENABLED");

        UserDTO target = BeanUtil.copyToBean(source, UserDTO.class);

        assertEquals(1008L, target.getId());
        assertEquals(27, target.getAge());
        assertTrue(target.getEnabled());
        assertEquals(LoginStatus.ENABLED, target.getStatus());
    }

    @Test
    @DisplayName("转换常用基础类型")
    void shouldConvertCommonTypes() throws Exception {
        UUID uuid = UUID.randomUUID();

        assertEquals(1, BeanUtil.convertValue("1", Integer.class));
        assertEquals(1L, BeanUtil.convertValue("1", Long.class));
        assertEquals(new BigInteger("123"), BeanUtil.convertValue("123", BigInteger.class));
        assertEquals(new BigDecimal("12.30"), BeanUtil.convertValue("12.30", BigDecimal.class));
        assertTrue(BeanUtil.convertValue("是", Boolean.class));
        assertEquals('A', BeanUtil.convertValue("A", Character.class));
        assertEquals(LoginStatus.ENABLED, BeanUtil.convertValue("ENABLED", LoginStatus.class));
        assertEquals(uuid, BeanUtil.convertValue(uuid.toString(), UUID.class));
        assertEquals(URI.create("https://example.com"), BeanUtil.convertValue("https://example.com", URI.class));
        assertEquals(new URL("https://example.com"), BeanUtil.convertValue("https://example.com", URL.class));
    }

    @Test
    @DisplayName("转换时间类型")
    void shouldConvertDateTimeTypes() {
        LocalDateTime dateTime = BeanUtil.convertValue("2026-04-29 12:30:00", LocalDateTime.class);
        LocalDate date = BeanUtil.convertValue("2026-04-29", LocalDate.class);
        LocalTime time = BeanUtil.convertValue("12:30:00", LocalTime.class);
        Instant instant = BeanUtil.convertValue(dateTime, Instant.class);
        Date legacyDate = BeanUtil.convertValue(dateTime, Date.class);

        assertEquals(LocalDateTime.of(2026, 4, 29, 12, 30), dateTime);
        assertEquals(LocalDate.of(2026, 4, 29), date);
        assertEquals(LocalTime.of(12, 30), time);
        assertNotNull(instant);
        assertNotNull(legacyDate);
    }

    @Test
    @DisplayName("转换失败时返回默认值")
    void shouldReturnDefaultValueWhenConversionFailed() {
        Integer value = BeanUtil.convertValue("not-number", Integer.class, -1);

        assertEquals(-1, value);
        assertFalse(BeanUtil.canConvertValue("not-number", Integer.class));
        assertTrue(BeanUtil.isSupportedConvertType(LocalDate.class));
    }
}
