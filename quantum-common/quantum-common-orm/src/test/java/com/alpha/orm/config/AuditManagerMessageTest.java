package com.alpha.orm.config;

import com.mybatisflex.core.audit.AuditMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class AuditManagerMessageTest {

    @Test
    void slowQueryAuditReadsTheSqlTemplateWithoutInterpolatingParameters() {
        var message = spy(new AuditMessage());
        message.setQuery("SELECT id FROM sys_user WHERE username = ?");
        message.setQueryParams(List.of("synthetic-account"));
        message.setElapsedTime(1_500);

        new AuditManagerMessage().collect(message);

        verify(message).getQuery();
        verify(message, never()).getFullSql();
    }
}
