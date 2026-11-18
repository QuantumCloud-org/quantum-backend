package com.alpha.system.service.impl;

import com.alpha.file.entity.FileInfo;
import com.alpha.file.service.FileStorageService;
import com.alpha.system.domain.SysFile;
import com.mybatisflex.core.dialect.DbType;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.alpha.system.domain.table.SysFileTableDef.SYS_FILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysFileServiceImplTest {
    private final FileStorageService storage = mock(FileStorageService.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final SysFileServiceImpl service = spy(new SysFileServiceImpl(storage, transactions));

    @BeforeEach
    void begin() {
        TransactionSynchronizationManager.initSynchronization();
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @AfterEach
    void clear() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void keepsUploadedObjectWhenCommitOutcomeIsUnknown() {
        uploadFile();

        complete(TransactionSynchronization.STATUS_UNKNOWN);

        verify(storage, never()).delete(any());
    }

    @Test
    void compensatesOnlyAnExplicitUploadRollback() {
        uploadFile();

        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storage).delete("files/uploaded");
    }

    @Test
    void survivingMetadataCanDownloadAfterDeletingAnotherRowForTheSamePath() throws IOException {
        var first = file(2L);
        var survivor = file(3L);
        var contents = new AtomicReference<>(new byte[]{1, 2, 3});
        doReturn(List.of(first)).when(service).listByIds(List.of(2L));
        doReturn(true).when(service).removeByIds(List.of(2L));
        doReturn(survivor).when(service).getById(3L);
        doReturn(true).when(service).exists(any(QueryWrapper.class));
        when(storage.delete("files/shared")).thenAnswer(invocation -> contents.getAndSet(null) != null);
        when(storage.download("files/shared")).thenAnswer(invocation -> new ByteArrayInputStream(contents.get()));

        assertThat(service.deleteByIds(List.of(2L))).isEqualTo(1);
        verify(service, never()).exists(any(QueryWrapper.class));
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(transactions).getTransaction(argThat(definition -> definition.isReadOnly()
                && definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        verify(service).exists(argThat((QueryWrapper query) ->
                query.from(SYS_FILE).toSQL(DbType.POSTGRE_SQL).contains("\"file_path\" = 'files/shared'")));
        try (var input = service.downloadById(3L)) {
            assertThat(input.readAllBytes()).containsExactly(1, 2, 3);
        }
        verify(storage, never()).delete("files/shared");
    }

    @Test
    void deletesAllMetadataForADuplicatePathAndCleansTheObjectOnceAfterCommit() {
        var first = file(2L);
        var second = file(3L);
        doReturn(List.of(first, second)).when(service).list(any(QueryWrapper.class));
        doReturn(true).when(service).removeByIds(any());
        doReturn(false).when(service).exists(any(QueryWrapper.class));
        when(storage.delete("files/shared")).thenReturn(true);

        assertThat(service.deleteByPath(" files/shared ")).isTrue();
        verify(service).removeByIds(List.of(2L, 3L));
        verify(storage, never()).delete(any());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(storage).delete("files/shared");
    }

    private void uploadFile() {
        var input = new MockMultipartFile("file", new byte[]{1});
        var uploaded = new FileInfo();
        uploaded.setFilePath("files/uploaded");
        when(storage.upload(input, "files")).thenReturn(uploaded);
        when(storage.delete("files/uploaded")).thenReturn(true);
        doReturn(true).when(service).save(any(SysFile.class));
        service.upload(input, "other", 5L, "files");
    }

    private void complete(int status) {
        TransactionSynchronizationManager.getSynchronizations().forEach(item -> item.afterCompletion(status));
    }

    private static SysFile file(long id) {
        var file = new SysFile();
        file.setId(id);
        file.setFilePath("files/shared");
        return file;
    }
}
