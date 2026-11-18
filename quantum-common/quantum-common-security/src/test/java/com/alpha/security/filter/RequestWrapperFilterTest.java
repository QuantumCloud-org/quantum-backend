package com.alpha.security.filter;

import com.alpha.framework.config.JacksonConfig;
import com.alpha.security.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequestWrapperFilterTest {

    @Test
    void limitsJsonBodyIncludingRequestsWithoutKnownLength() throws Exception {
        var request = new MockHttpServletRequest("POST", "/resource") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContentType("application/json");
        request.setContent(new JacksonConfig().jsonMapper().writeValueAsBytes(java.util.Map.of("notes", "a".repeat(2 * 1024 * 1024))));
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        filter(new SecurityProperties()).doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void keepsPasswordsAndRichTextForExcludedRoutes() throws Exception {
        var properties = new SecurityProperties();
        properties.setXssExcludes(List.of("/rich-text"));
        var mapper = new JacksonConfig();

        var request = new MockHttpServletRequest("POST", "/rich-text");
        request.setContentType("application/json");
        request.setContent("""
            {"newPassword":"pass<a>word","notes":"<p>Hello</p>"}
            """.getBytes(StandardCharsets.UTF_8));
        filter(properties).doFilter(request, new MockHttpServletResponse(), (wrapped, response) -> {
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes((jakarta.servlet.http.HttpServletRequest) wrapped));
            try {
                var body = mapper.jsonMapper().readValue(wrapped.getInputStream(), Body.class);
                assertThat(body.newPassword()).isEqualTo("pass<a>word");
                assertThat(body.notes()).isEqualTo("<p>Hello</p>");
            } finally {
                RequestContextHolder.resetRequestAttributes();
            }
        });
    }

    private RequestWrapperFilter filter(SecurityProperties properties) {
        return new RequestWrapperFilter(properties,
                new com.alpha.framework.util.ServletJsonWriter(new JacksonConfig().jsonMapper()));
    }

    record Body(String newPassword, String notes) {
    }
}
