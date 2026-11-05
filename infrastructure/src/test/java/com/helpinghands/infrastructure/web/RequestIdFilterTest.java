package com.helpinghands.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    private String run(MockHttpServletRequest request, MockHttpServletResponse response,
                       AtomicReference<String> mdcInsideChain) throws Exception {
        FilterChain chain = (req, res) -> mdcInsideChain.set(MDC.get(RequestIdFilter.MDC_KEY));
        filter.doFilter(request, response, chain);
        return response.getHeader(RequestIdFilter.HEADER);
    }

    @Test
    void shouldGenerateRequestIdWhenMissing() throws Exception {
        var inside = new AtomicReference<String>();

        String header = run(new MockHttpServletRequest(), new MockHttpServletResponse(), inside);

        assertThat(header).isNotBlank();
        assertThat(inside.get()).isEqualTo(header);
    }

    @Test
    void shouldReuseSafeIncomingRequestId() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "client-id_42");
        var inside = new AtomicReference<String>();

        assertThat(run(request, new MockHttpServletResponse(), inside)).isEqualTo("client-id_42");
        assertThat(inside.get()).isEqualTo("client-id_42");
    }

    @Test
    void shouldReplaceUnsafeIncomingRequestId() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "bad id\nwith newline");

        String header = run(request, new MockHttpServletResponse(), new AtomicReference<>());

        assertThat(header).doesNotContain("bad").matches("[A-Za-z0-9-]{36}");
    }

    @Test
    void shouldClearMdcAfterRequest() throws Exception {
        run(new MockHttpServletRequest(), new MockHttpServletResponse(), new AtomicReference<>());

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }
}
