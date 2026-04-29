package io.github.atengk.zip;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class ZipUtilTestSupport {

    private ZipUtilTestSupport() {
    }

    static Path text(Path dir, String relative, String content) throws IOException {
        Path file = dir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    static Path sampleDir(Path tempDir) throws IOException {
        Path root = tempDir.resolve("source");
        text(root, "a.txt", "A");
        text(root, "nested/b.txt", "B");
        return root;
    }
}
