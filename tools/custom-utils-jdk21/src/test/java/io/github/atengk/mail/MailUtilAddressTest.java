package io.github.atengk.mail;

import io.github.atengk.utils.mail.MailUtil;
import jakarta.mail.internet.InternetAddress;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MailUtilAddressTest {

    @Test
    void isValidAddressShouldReturnTrueForValidAddress() {
        assertTrue(MailUtil.isValidAddress("user@example.com"));
    }

    @Test
    void isValidAddressShouldReturnFalseForInvalidAddress() {
        assertFalse(MailUtil.isValidAddress("invalid"));
        assertFalse(MailUtil.isValidAddress(null));
    }

    @Test
    void parseAndFormatAddressShouldWork() {
        InternetAddress address = MailUtil.parseAddress("User@Example.com");
        assertEquals("User@Example.com", address.getAddress());
        assertTrue(MailUtil.formatAddress("user@example.com", "测试用户").contains("user@example.com"));
    }

    @Test
    void splitJoinAndDeduplicateShouldWork() {
        List<String> addresses = MailUtil.splitAddresses("a@example.com;b@example.com,a@example.com");
        assertEquals(3, addresses.size());
        assertEquals("a@example.com,b@example.com", MailUtil.joinAddresses(addresses));
    }

    @Test
    void parseAddressShouldRejectBlank() {
        assertThrows(IllegalArgumentException.class, () -> MailUtil.parseAddress(" "));
    }
}
