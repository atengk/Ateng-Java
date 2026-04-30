package io.github.atengk.random;

import io.github.atengk.utils.random.RandomUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilMockDataTest {

    @Test
    void shouldGenerateMockUserData() {
        assertTrue(RandomUtil.randomMobile().matches("1\\d{10}"));
        assertTrue(RandomUtil.randomEmail().endsWith("@example.com"));
        assertTrue(RandomUtil.randomEmail("test.com").endsWith("@test.com"));
        assertTrue(RandomUtil.randomUsername().matches("user[a-z]{4}\\d{4}"));
        assertFalse(RandomUtil.randomNickname().isBlank());
        assertFalse(RandomUtil.randomChineseName().isBlank());
        assertTrue(RandomUtil.randomEnglishName().contains(" "));
        assertTrue(RandomUtil.randomGender().matches("男|女"));
    }

    @Test
    void shouldGenerateAgeAndIdCardLikeData() {
        int age = RandomUtil.randomAge(18, 20);
        assertTrue(age >= 18 && age <= 20);
        assertTrue(RandomUtil.randomIdCardLike().matches("\\d{17}[0-9X]"));
    }

    @Test
    void shouldRejectInvalidMockDataArguments() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomEmail(""));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomAge(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomAge(20, 10));
    }
}
