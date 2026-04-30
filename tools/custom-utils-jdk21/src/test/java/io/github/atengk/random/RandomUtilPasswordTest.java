package io.github.atengk.random;

import io.github.atengk.utils.random.RandomUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilPasswordTest {

    @Test
    void shouldGeneratePasswords() {
        assertEquals(10, RandomUtil.randomPassword(10).length());
        assertEquals(10, RandomUtil.randomPassword(10, false).length());
        assertEquals(12, RandomUtil.randomStrongPassword(12).length());
        assertEquals(8, RandomUtil.randomReadablePassword(8).length());
        assertTrue("!@#$%^&*()-_=+[]{};:,.?".indexOf(RandomUtil.randomSpecialChar()) >= 0);
    }

    @Test
    void shouldGeneratePasswordWithCustomRule() {
        RandomUtil.PasswordRule rule = new RandomUtil.PasswordRule(6, true, false, true, false, "0", 2);
        String password = RandomUtil.randomPasswordWithRule(rule);
        assertEquals(6, password.length());
        assertFalse(password.contains("0"));
        assertEquals(6, RandomUtil.PasswordRule.defaultRule(6).getLength());
    }

    @Test
    void shouldRejectInvalidPasswordRule() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomPassword(0));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomStrongPassword(3));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomPasswordWithRule(new RandomUtil.PasswordRule(6, false, false, false, false, "", 1)));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomPasswordWithRule(new RandomUtil.PasswordRule(2, true, true, true, true, "", 4)));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomPasswordWithRule(new RandomUtil.PasswordRule(6, true, false, false, false, "abcdefghijklmnopqrstuvwxyz", 1)));
    }
}
