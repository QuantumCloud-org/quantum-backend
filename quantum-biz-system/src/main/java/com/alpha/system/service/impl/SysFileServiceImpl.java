package com.alpha.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alpha.file.entity.FileInfo;
import com.alpha.file.service.FileStorageService;
import com.alpha.framework.context.UserContext;
import com.alpha.framework.exception.BizException;
import com.alpha.system.domain.SysFile;
import com.alpha.system.dto.request.SysFileQuery;
import com.alpha.system.mapper.SysFileMapper;
import com.alpha.system.service.ISysFileService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.alpha.system.domain.table.SysFileTableDef.SYS_FILE;

/**
 * 文件管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysFileServiceImpl extends ServiceImpl<SysFileMapper, SysFile> implements ISysFileService {

    private final FileStorageService fileStorageService;
    private final PlatformTransactionManager transactionManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFile upload(MultipartFile file, String bizType, Long bizId, String path) {
        String normalizedBiz = normalizeBizType(bizType);
        FileInfo info = fileStorageService.upload(file, path);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    cleanupUploadedFile(info);
                } else if (status == STATUS_UNKNOWN) {
                    log.warn("上传事务结果未知，保留文件待对账 | path={} | biz={}:{}",
                            info.getFilePath(), normalizedBiz, bizId);
                }
            }
        });
        return saveFromFileInfo(info, normalizedBiz, bizId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SysFile> uploadBatch(List<MultipartFile> files, String bizType, Long bizId, String path) {
        if (files == null || files.isEmpty()) {
            throw new BizException("文件列表不能为空");
        }
        List<SysFile> result = new ArrayList<>(files.size());
        for (MultipartFile f : files) {
            result.add(upload(f, bizType, bizId, path));
        }
        return result;
    }

    @Override
    public SysFile selectById(Long id) {
        SysFile entity = getById(id);
        if (entity == null) {
            throw new BizException("文件不存在");
        }
        return entity;
    }

    @Override
    public Page<SysFile> selectFilePage(SysFileQuery query) {
        return page(query.getPage(), buildQueryWrapper(query));
    }

    @Override
    public InputStream downloadById(Long id) {
        SysFile entity = selectById(id);
        return fileStorageService.download(entity.getFilePath());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return deleteFiles(listByIds(ids));
    }

    private int deleteFiles(List<SysFile> files) {
        if (files.isEmpty()) {
            return 0;
        }
        if (!removeByIds(files.stream().map(SysFile::getId).toList())) {
            throw new BizException("文件记录删除失败");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // The committing transaction's resources are still bound during this callback.
                var readReferences = new TransactionTemplate(transactionManager);
                readReferences.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                readReferences.setReadOnly(true);
                files.stream().map(SysFile::getFilePath).distinct()
                        .forEach(path -> deleteUnreferencedObject(path, readReferences));
            }
        });
        return files.size();
    }

    private void deleteUnreferencedObject(String path, TransactionTemplate readReferences) {
        try {
            Boolean referenced = readReferences.execute(status ->
                    exists(QueryWrapper.create().where(SYS_FILE.FILE_PATH.eq(path))));
            if (Boolean.FALSE.equals(referenced) && !fileStorageService.delete(path)) {
                log.warn("文件记录已删除，存储清理失败 | path={}", path);
            }
        } catch (RuntimeException e) {
            log.warn("文件记录已删除，存储清理失败 | path={}", path, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByPath(String path) {
        if (StrUtil.isBlank(path)) {
            return false;
        }
        List<SysFile> files = list(QueryWrapper.create().where(SYS_FILE.FILE_PATH.eq(path.trim())));
        return deleteFiles(files) > 0;
    }

    @Override
    public boolean existsByPath(String path) {
        if (StrUtil.isBlank(path)) {
            return false;
        }
        SysFile entity = findByPath(path);
        return entity != null && fileStorageService.exists(entity.getFilePath());
    }

    private SysFile findByPath(String path) {
        return getOne(QueryWrapper.create().where(SYS_FILE.FILE_PATH.eq(path.trim())).limit(1));
    }

    @Override
    public SysFile saveFromFileInfo(FileInfo info, String bizType, Long bizId) {
        String normalizedBiz = normalizeBizType(bizType);
        SysFile entity = new SysFile();
        entity.setOriginalName(info.getOriginalName());
        entity.setStorageName(info.getStorageName());
        entity.setFilePath(info.getFilePath());
        entity.setUrl(info.getUrl());
        entity.setSize(info.getSize());
        entity.setExtension(info.getExtension());
        entity.setContentType(info.getContentType());
        entity.setMd5(info.getMd5());
        entity.setStorageType(info.getStorageType());
        entity.setBucket(info.getBucket());
        entity.setBizType(normalizedBiz);
        entity.setBizId(bizId);
        entity.setUploadBy(UserContext.getUserId());
        entity.setUploadTime(info.getUploadTime());
        save(entity);
        log.info("文件落库 | id={} | name={} | size={}B | biz={}:{}",
                entity.getId(), entity.getOriginalName(), entity.getSize(), normalizedBiz, bizId);
        return entity;
    }

    private String normalizeBizType(String bizType) {
        if (StrUtil.isBlank(bizType)) {
            return "other";
        }
        String normalized = bizType.trim().toLowerCase();
        if (!ALLOWED_BIZ_TYPES.contains(normalized)) {
            throw new BizException("非法的业务类型: " + bizType);
        }
        return normalized;
    }

    private void cleanupUploadedFile(FileInfo info) {
        if (info == null || StrUtil.isBlank(info.getFilePath())) {
            return;
        }
        try {
            if (!fileStorageService.delete(info.getFilePath())) {
                log.warn("上传失败后的文件补偿删除未成功 | path={}", info.getFilePath());
            }
        } catch (Exception cleanupError) {
            log.warn("上传失败后的文件补偿删除异常 | path={}", info.getFilePath(), cleanupError);
        }
    }

    private QueryWrapper buildQueryWrapper(SysFileQuery query) {
        return QueryWrapper.create()
                .where(SYS_FILE.ORIGINAL_NAME.like(query.getOriginalName())
                        .when(StrUtil.isNotBlank(query.getOriginalName())))
                .and(SYS_FILE.EXTENSION.eq(query.getExtension())
                        .when(StrUtil.isNotBlank(query.getExtension())))
                .and(SYS_FILE.BIZ_TYPE.eq(query.getBizType())
                        .when(StrUtil.isNotBlank(query.getBizType())))
                .and(SYS_FILE.BIZ_ID.eq(query.getBizId())
                        .when(query.getBizId() != null))
                .and(SYS_FILE.UPLOAD_BY.eq(query.getUploadBy())
                        .when(query.getUploadBy() != null))
                .and(SYS_FILE.UPLOAD_TIME.ge(query.getBeginTime())
                        .when(query.getBeginTime() != null))
                .and(SYS_FILE.UPLOAD_TIME.le(query.getEndTime())
                        .when(query.getEndTime() != null))
                .orderBy(SYS_FILE.UPLOAD_TIME.desc());
    }
}
