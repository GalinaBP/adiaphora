package ru.adiaphora.platform.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CorrelationIdFilter}: a well-formed inbound id is reused, while an untrusted
 * (over-length or control-character) id is rejected in favour of a freshly generated one. This is the
 * control that stops an attacker-supplied header from forging logs or overflowing the audit column.
 */
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void reusesWellFormedInboundId() throws Exception {
        String inbound = "abc-123_DEF.456";
        MockHttpServletResponse response = invokeWithHeader(inbound);

        assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo(inbound);
    }

    @Test
    void generatesFreshIdWhenHeaderMissing() throws Exception {
        MockHttpServletResponse response = invokeWithHeader(null);

        String emitted = response.getHeader(CorrelationId.HEADER);
        assertThat(emitted).isNotBlank();
        assertThat(emitted.length()).isLessThanOrEqualTo(CorrelationId.MAX_LENGTH);
    }

    @Test
    void rejectsOverLengthInboundId() throws Exception {
        String tooLong = "a".repeat(CorrelationId.MAX_LENGTH + 1);
        MockHttpServletResponse response = invokeWithHeader(tooLong);

        String emitted = response.getHeader(CorrelationId.HEADER);
        assertThat(emitted).isNotEqualTo(tooLong);
        assertThat(emitted.length()).isLessThanOrEqualTo(CorrelationId.MAX_LENGTH);
    }

    @Test
    void rejectsInboundIdWithControlCharacters() throws Exception {
        String forged = "ok\r\nInjected: value";
        MockHttpServletResponse response = invokeWithHeader(forged);

        assertThat(response.getHeader(CorrelationId.HEADER)).isNotEqualTo(forged);
    }

    private MockHttpServletResponse invokeWithHeader(String inbound) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/anything");
        if (inbound != null) {
            request.addHeader(CorrelationId.HEADER, inbound);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
