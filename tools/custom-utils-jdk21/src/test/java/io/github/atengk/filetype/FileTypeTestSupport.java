package io.github.atengk.filetype;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class FileTypeTestSupport {

    static final byte[] PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
    };

    static final byte[] PDF_BYTES = "%PDF-1.7\n1 0 obj\n<<>>\nendobj\n".getBytes(StandardCharsets.UTF_8);

    static final byte[] TEXT_BYTES = "hello file type util".getBytes(StandardCharsets.UTF_8);

    private FileTypeTestSupport() {
    }

    static Path writeFile(Path dir, String fileName, byte[] bytes) throws IOException {
        Path path = dir.resolve(fileName);
        Files.write(path, bytes);
        return path;
    }
}
