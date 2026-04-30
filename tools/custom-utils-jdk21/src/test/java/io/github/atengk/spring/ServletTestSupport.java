package io.github.atengk.spring;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

final class ServletTestSupport {
    private ServletTestSupport() {
    }

    static ServletPair bind() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        return new ServletPair(request, response);
    }

    static void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    record ServletPair(MockHttpServletRequest request, MockHttpServletResponse response) {
    }
}
