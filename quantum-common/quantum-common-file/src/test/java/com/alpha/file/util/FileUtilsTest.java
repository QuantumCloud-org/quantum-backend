package com.alpha.file.util;

import com.alpha.framework.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileUtilsTest {

    @Test
    void validateMagicBytesAcceptsValidWebpHeader() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.webp",
                "image/webp",
                new byte[]{0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50}
        );

        assertDoesNotThrow(() -> FileUtils.validateMagicBytes(file, "webp"));
    }

    @Test
    void validateMagicBytesRejectsNonWebpRiffContainer() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.webp",
                "image/webp",
                new byte[]{0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00, 0x57, 0x41, 0x56, 0x45}
        );

        assertThrows(BizException.class, () -> FileUtils.validateMagicBytes(file, "webp"));
    }
}
