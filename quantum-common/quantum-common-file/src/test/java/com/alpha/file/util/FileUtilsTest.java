package com.alpha.file.util;

import com.alpha.framework.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUtilsTest {

    @Test
    void validateObjectKeyPartsShouldAllowNormalRelativeKey() {
        assertThatCode(() -> FileUtils.validateObjectKeyParts("biz/reports", "safe.pdf"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateObjectKeyPartsShouldRejectTraversalPath() {
        assertThatThrownBy(() -> FileUtils.validateObjectKeyParts("../admin", "safe.pdf"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("非法文件路径");
    }

    @Test
    void validateObjectKeyPartsShouldRejectFileNameWithSeparators() {
        assertThatThrownBy(() -> FileUtils.validateObjectKeyParts("biz/reports", "../evil.pdf"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("非法文件名");

        assertThatThrownBy(() -> FileUtils.validateObjectKeyParts("biz/reports", "nested/evil.pdf"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("非法文件名");
    }
}
