package io.github.atengk.spring;

import io.github.atengk.utils.spring.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServletForwardRedirectTest {

    @AfterEach
    void tearDown() {
        ServletTestSupport.clear();
    }

    @Test
    void shouldForwardRedirectAndSendError() throws Exception {
        ServletTestSupport.ServletPair pair = ServletTestSupport.bind();
        SpringUtil.forward("/target");
        assertEquals("/target", pair.response().getForwardedUrl());

        ServletTestSupport.clear();
        pair = ServletTestSupport.bind();
        SpringUtil.redirect("/login");
        assertEquals("/login", pair.response().getRedirectedUrl());

        ServletTestSupport.clear();
        pair = ServletTestSupport.bind();
        SpringUtil.sendError(401, "no auth");
        assertEquals(401, pair.response().getStatus());
        assertEquals("no auth", pair.response().getErrorMessage());

        ServletTestSupport.clear();
        pair = ServletTestSupport.bind();
        SpringUtil.sendError(403);
        assertEquals(403, pair.response().getStatus());
    }

    @Test
    void shouldRejectInvalidForwardRedirectArguments() {
        ServletTestSupport.bind();
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.redirect(""));
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.forward(" "));
    }
}
