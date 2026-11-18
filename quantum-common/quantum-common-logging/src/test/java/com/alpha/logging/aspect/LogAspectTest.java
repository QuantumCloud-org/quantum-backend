package com.alpha.logging.aspect;

import com.alpha.framework.util.JsonUtil;
import com.alpha.logging.annotation.SystemLog;
import com.alpha.logging.event.OperLogEvent;
import com.alpha.logging.properties.OperLogProperties;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class LogAspectTest {

    @Test
    void masksSensitiveArgumentNamesAndValuesThatCouldNotBeSanitized() throws Exception {
        var mapper = spy(JsonMapper.builder().build());
        var events = new ArrayList<Object>();
        var aspect = new LogAspect(new JsonUtil(mapper), events::add, new OperLogProperties());
        aspect.initSensitiveAwareMapper();
        var arg = Map.of("password", "synthetic-password");
        doThrow(new IllegalArgumentException("cannot convert")).when(mapper).valueToTree(arg);

        aspect.afterReturning(point(new String[]{"accessToken", "body"},
                new Object[]{"synthetic-token", arg}), systemLog(), null);

        var entry = ((OperLogEvent) events.getFirst()).getOperLog();
        assertThat(entry.getOperParam())
                .contains("\"accessToken\":\"[PROTECTED]\"", "\"body\":\"[PROTECTED]\"")
                .doesNotContain("synthetic-token", "synthetic-password");
    }

    @Test
    void omitsResponseWhenFilteredSerializationFails() throws Exception {
        var events = new ArrayList<Object>();
        var aspect = new LogAspect(new JsonUtil(JsonMapper.builder().build()), events::add, new OperLogProperties());
        aspect.initSensitiveAwareMapper();
        var result = Map.of("accessToken", "synthetic-token");
        var filteredMapper = mock(ObjectMapper.class);
        when(filteredMapper.writeValueAsString(result)).thenThrow(new IllegalArgumentException("cannot serialize"));
        ReflectionTestUtils.setField(aspect, "sensitiveAwareMapper", filteredMapper);

        aspect.afterReturning(point(new String[0], new Object[0]), systemLog(), result);

        var entry = ((OperLogEvent) events.getFirst()).getOperLog();
        assertThat(entry.getJsonResult()).isNull();
    }

    private JoinPoint point(String[] parameterNames, Object[] arguments) {
        var signature = mock(MethodSignature.class);
        when(signature.getName()).thenReturn("operation");
        when(signature.getParameterNames()).thenReturn(parameterNames);
        var point = mock(JoinPoint.class);
        when(point.getSignature()).thenReturn(signature);
        when(point.getArgs()).thenReturn(arguments);
        when(point.getTarget()).thenReturn(this);
        return point;
    }

    private static SystemLog systemLog() throws NoSuchMethodException {
        return LogAspectTest.class.getDeclaredMethod("operation").getAnnotation(SystemLog.class);
    }

    @SystemLog(saveResult = true)
    private void operation() {
    }
}
