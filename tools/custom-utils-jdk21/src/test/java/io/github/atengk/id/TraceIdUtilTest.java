package io.github.atengk.id;

import io.github.atengk.utils.id.IdUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TraceIdUtilTest {

    @Test
    void shouldGenerateTraceIdAndRequestId() {
        String traceId = IdUtil.traceId();
        String requestId = IdUtil.requestId();
        assertTrue(IdUtil.isTraceId(traceId));
        assertTrue(IdUtil.isTraceId(requestId));
        assertEquals(32, traceId.length());
    }

    @Test
    void shouldGenerateShortTraceAndSpan() {
        assertTrue(IdUtil.isTraceId(IdUtil.traceIdShort()));
        assertTrue(IdUtil.spanId().matches("^[0-9a-f]{16}$"));
        assertTrue(IdUtil.parentSpanId().matches("^[0-9a-f]{16}$"));
    }

    @Test
    void shouldEnsureTraceId() {
        String valid = IdUtil.traceId();
        assertEquals(valid, IdUtil.ensureTraceId(valid));
        assertTrue(IdUtil.isTraceId(IdUtil.ensureTraceId(null)));
        assertTrue(IdUtil.isTraceId(IdUtil.getOrCreateTraceId("bad")));
    }

    @Test
    void shouldRejectInvalidTraceId() {
        assertFalse(IdUtil.isTraceId(null));
        assertFalse(IdUtil.isTraceId("1234567"));
        assertFalse(IdUtil.isTraceId("invalid trace id"));
    }
}
