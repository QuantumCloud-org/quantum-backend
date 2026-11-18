package com.alpha.logging.filter;

import com.alpha.framework.constant.CommonConstants;
import com.alpha.framework.util.JsonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class TraceFilterTest {
    @Test
    void replacesTraceIdOutsideAcceptedLength() throws Exception {
        var request = new MockHttpServletRequest("GET", "/resource");
        request.addHeader(CommonConstants.HEADER_TRACE_ID, "short");
        var response = new MockHttpServletResponse();
        new TraceFilter(new JsonUtil(JsonMapper.builder().build())).doFilter(request, response, new MockFilterChain());
        assertThat(response.getHeader(CommonConstants.HEADER_TRACE_ID)).matches("[A-Za-z0-9-]{8,64}");
    }
}
