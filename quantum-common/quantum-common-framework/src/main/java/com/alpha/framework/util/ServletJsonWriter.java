package com.alpha.framework.util;

import com.alpha.framework.entity.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class ServletJsonWriter {

    private final ObjectMapper mapper;

    /** Writes a standard JSON response from outside Spring MVC. */
    public void write(HttpServletResponse response, int status, Result<?> result) throws IOException {
        String json = mapper.writeValueAsString(result);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json);
    }
}
