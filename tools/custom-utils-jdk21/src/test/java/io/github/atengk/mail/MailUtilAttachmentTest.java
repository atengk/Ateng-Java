package io.github.atengk.mail;

import io.github.atengk.utils.mail.MailUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MailUtilAttachmentTest {

    @TempDir
    Path tempDir;

    @Test
    void buildByteAttachmentShouldWork() {
        MailUtil.MailAttachment attachment = MailUtil.buildAttachment("a.txt", "hello".getBytes(), "text/plain");
        assertEquals("a.txt", MailUtil.getAttachmentName(attachment));
        assertDoesNotThrow(() -> MailUtil.validateAttachment(attachment));
    }

    @Test
    void buildFileAttachmentShouldWork() throws Exception {
        Path file = tempDir.resolve("a.txt");
        Files.writeString(file, "hello");
        MailUtil.MailAttachment attachment = MailUtil.buildAttachment(file);
        assertEquals("a.txt", attachment.fileName);
    }

    @Test
    void addStreamAttachmentShouldReadInputStream() {
        MailUtil.MailMessage message = TestFixtures.message();
        MailUtil.addStreamAttachment(message, "a.txt", new ByteArrayInputStream("abc".getBytes()), "text/plain");
        assertEquals(1, message.attachments.size());
    }

    @Test
    void removeAttachmentShouldReturnTrueWhenExists() {
        MailUtil.MailMessage message = TestFixtures.message();
        MailUtil.addByteAttachment(message, "a.txt", "abc".getBytes(), "text/plain");
        assertTrue(MailUtil.removeAttachment(message, "a.txt"));
    }

    @Test
    void validateAttachmentTypeShouldRejectUnsupportedType() {
        MailUtil.MailAttachment attachment = MailUtil.buildAttachment("a.exe", new byte[]{1}, "application/x-msdownload");
        assertThrows(IllegalArgumentException.class, () -> MailUtil.validateAttachmentType(attachment, List.of("text/plain")));
    }
}
