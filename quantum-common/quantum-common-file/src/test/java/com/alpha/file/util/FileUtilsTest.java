package com.alpha.file.util;

import com.alpha.framework.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUtilsTest {

    @Test
    void validatesMagicBytesWhenTheUploadStreamReadsInChunks() {
        byte[] content = "%PDF-example".getBytes(StandardCharsets.US_ASCII);
        var file = new MockMultipartFile("file", "report.pdf", "application/pdf", content) {
            @Override
            public InputStream getInputStream() {
                var source = new ByteArrayInputStream(content);
                return new InputStream() {
                    @Override
                    public int read() {
                        return source.read();
                    }

                    @Override
                    public int read(byte[] bytes, int offset, int length) {
                        return source.read(bytes, offset, Math.min(length, 2));
                    }
                };
            }
        };

        assertThatCode(() -> FileUtils.validateMagicBytes(file, "pdf")).doesNotThrowAnyException();
    }

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
