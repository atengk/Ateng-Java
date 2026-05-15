package io.github.atengk.http;

import io.github.atengk.utils.http.HttpUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HttpSecurityTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldValidateUrlSecurityAndPathTraversal() {
        assertThrows(HttpUtil.HttpSecurityException.class, () -> HttpUtil.validateUrlBySecurity("ftp://example.com/a", HttpUtil.SecurityOptions.defaults()));
        assertThrows(HttpUtil.HttpSecurityException.class, () -> HttpUtil.validateUrlBySecurity("http://127.0.0.1/a", HttpUtil.SecurityOptions.defaults().enableBlockPrivateAddress()));
        assertThrows(HttpUtil.HttpSecurityException.class, () -> HttpUtil.validateUrlBySecurity("http://bad.com/a", HttpUtil.SecurityOptions.defaults().withAllowedHosts(Set.of("good.com"))));
        assertThrows(HttpUtil.HttpSecurityException.class, () -> HttpUtil.validatePathInsideBase(tempDir, tempDir.resolve("../evil.txt")));
    }
}
