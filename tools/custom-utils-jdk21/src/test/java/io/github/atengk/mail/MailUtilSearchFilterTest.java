package io.github.atengk.mail;

import io.github.atengk.utils.mail.MailUtil;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MailUtilSearchFilterTest {

    @Test
    void filterMessagesShouldFilterByPredicate() throws Exception {
        MimeMessage a = TestFixtures.mimeMessage("A", "正文");
        MimeMessage b = TestFixtures.mimeMessage("B", "正文");
        List<Message> result = MailUtil.filterMessages(new Message[]{a, b}, message -> {
            try {
                return "A".equals(message.getSubject());
            } catch (Exception e) {
                return false;
            }
        });
        assertEquals(1, result.size());
    }

    @Test
    void sortMessagesShouldSortMessages() throws Exception {
        MimeMessage a = TestFixtures.mimeMessage("B", "正文");
        MimeMessage b = TestFixtures.mimeMessage("A", "正文");
        List<Message> result = MailUtil.sortMessages(new Message[]{a, b}, Comparator.comparing(message -> {
            try {
                return message.getSubject();
            } catch (Exception e) {
                return "";
            }
        }));
        assertEquals("A", result.get(0).getSubject());
    }

    @Test
    void filterMessagesShouldRejectNullPredicate() {
        assertThrows(NullPointerException.class, () -> MailUtil.filterMessages(new Message[0], null));
    }

    @Test
    void searchBySubjectShouldRejectBlankSubject() {
        assertThrows(IllegalArgumentException.class, () -> MailUtil.searchBySubject(null, " "));
    }
}
