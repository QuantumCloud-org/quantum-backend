package com.alpha.system.security;

import com.alpha.framework.exception.BizException;
import com.alpha.framework.enums.ResultCode;
import com.alpha.system.dto.response.ImportRowResult;

import java.util.List;

/** Carries validation outcomes for an import whose database transaction must roll back. */
public class UserImportException extends BizException {
    private final List<ImportRowResult> rows;

    public UserImportException(List<ImportRowResult> rows) {
        super(ResultCode.PARAM_INVALID, rows.getFirst().message());
        this.rows = List.copyOf(rows);
    }

    public List<ImportRowResult> getRows() {
        return rows;
    }
}
