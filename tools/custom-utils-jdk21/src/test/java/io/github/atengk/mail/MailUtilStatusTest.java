package io.github.atengk.mail;

import io.github.atengk.utils.mail.MailUtil;
import jakarta.mail.Flags;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MailUtilStatusTest {

    @Test
    void markAsReadAndUnreadShouldChangeSeenFlag() throws Exception {
        MimeMessage message = TestFixtures.mimeMessage("主题", "正文");
        MailUtil.markAsRead(message);
        assertTrue(message.isSet(Flags.Flag.SEEN));
        MailUtil.markAsUnread(message);
        assertFalse(message.isSet(Flags.Flag.SEEN));
    }

    @Test
    void markAsFlaggedAndUnmarkShouldChangeFlag() throws Exception {
        MimeMessage message = TestFixtures.mimeMessage("主题", "正文");
        MailUtil.markAsFlagged(message);
        assertTrue(message.isSet(Flags.Flag.FLAGGED));
        MailUtil.unmarkFlagged(message);
        assertFalse(message.isSet(Flags.Flag.FLAGGED));
    }

    @Test
    void deleteAndRestoreShouldChangeDeletedFlag() throws Exception {
        MimeMessage message = TestFixtures.mimeMessage("主题", "正文");
        MailUtil.deleteMessage(message);
        assertTrue(message.isSet(Flags.Flag.DELETED));
        MailUtil.restoreMessage(message);
        assertFalse(message.isSet(Flags.Flag.DELETED));
    }

    @Test
    void setMessageFlagShouldRejectNullFlag() throws Exception {
        MimeMessage message = TestFixtures.mimeMessage("主题", "正文");
        assertThrows(NullPointerException.class, () -> MailUtil.setMessageFlag(message, null, true));
    }
}
