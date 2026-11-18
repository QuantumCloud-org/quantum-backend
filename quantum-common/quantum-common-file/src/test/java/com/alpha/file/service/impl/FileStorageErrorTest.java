package com.alpha.file.service.impl;

import com.alpha.file.config.FileProperties;
import com.alpha.framework.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileStorageErrorTest {

    private static final byte[] PDF_HEADER = "%PDF-example".getBytes(StandardCharsets.US_ASCII);

    @Test
    void localUploadFailureReturnsOnlyThePublicMessage(@TempDir Path directory) {
        var properties = new FileProperties();
        properties.getLocal().setBasePath(directory.toString());
        var storage = new LocalFileStorageServiceImpl(properties);
        var upload = new MockMultipartFile("file", "report.pdf", "application/pdf", PDF_HEADER) {
            private boolean checkedSignature;

            @Override
            public java.io.InputStream getInputStream() throws IOException {
                if (checkedSignature) {
                    throw new IOException("private storage location");
                }
                checkedSignature = true;
                return super.getInputStream();
            }
        };

        assertThatThrownBy(() -> storage.upload(upload, "reports"))
                .isInstanceOf(BizException.class)
                .hasMessage("文件上传失败");
    }

    @Test
    void computesTheDigestFromTheUploadedS3Stream() {
        var storage = new RustfsFileStorageServiceImpl(new FileProperties());
        var client = mock(S3Client.class);
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenAnswer(call -> {
            RequestBody body = call.getArgument(1);
            try (var stream = body.contentStreamProvider().newStream()) {
                org.assertj.core.api.Assertions.assertThat(stream.readAllBytes()).isEqualTo(PDF_HEADER);
            }
            return software.amazon.awssdk.services.s3.model.PutObjectResponse.builder().build();
        });
        ReflectionTestUtils.setField(storage, "s3Client", client);
        var upload = new MockMultipartFile("file", "report.pdf", "application/pdf", PDF_HEADER);

        org.assertj.core.api.Assertions.assertThat(storage.upload(upload, "reports").getMd5())
                .isEqualTo(cn.hutool.crypto.digest.DigestUtil.md5Hex(PDF_HEADER));
    }

    @Test
    void s3UploadFailureReturnsOnlyThePublicMessage() {
        var storage = new RustfsFileStorageServiceImpl(new FileProperties());
        var client = mock(S3Client.class);
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("private bucket details").build());
        ReflectionTestUtils.setField(storage, "s3Client", client);
        var upload = new MockMultipartFile("file", "report.pdf", "application/pdf", PDF_HEADER);

        assertThatThrownBy(() -> storage.upload(upload, "reports"))
                .isInstanceOf(BizException.class)
                .hasMessage("文件上传失败");
    }
}
