package io.github.atengk.mail;

import io.github.atengk.utils.mail.MailUtil;
import jakarta.mail.Folder;
import jakarta.mail.Store;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MailUtilFolderTest {

    @Test
    void getFolderShouldRejectNullStore() {
        assertThrows(NullPointerException.class, () -> MailUtil.getFolder(null, "INBOX"));
    }

    @Test
    void getFolderShouldRejectBlankName() {
        assertThrows(NullPointerException.class, () -> MailUtil.getFolder((Store) null, " "));
    }

    @Test
    void openFolderShouldRejectNullFolder() {
        assertThrows(NullPointerException.class, () -> MailUtil.openFolder(null, Folder.READ_ONLY));
    }

    @Test
    void closeFolderShouldIgnoreNullFolder() {
        assertDoesNotThrow(() -> MailUtil.closeFolder(null, false));
    }

    @Test
    void existsFolderShouldRejectNullStore() {
        assertThrows(NullPointerException.class, () -> MailUtil.existsFolder(null, "INBOX"));
    }
}
