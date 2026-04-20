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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.alpha.system.domain.table.SysFileTableDef.SYS_FILE;

/**
 * 文件管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysFileServiceImpl extends ServiceImpl<SysFileMapper, SysFile> implements ISysFileService {

    private final FileStorageService fileStorageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFile upload(MultipartFile file, String bizType, Long bizId, String path) {
        String normalizedBiz = normalizeBizType(bizType);
        FileInfo info = fileStorageService.upload(file, path);
        try {
            return saveFromFileInfo(info, normalizedBiz, bizId);
        } catch (Exception e) {
            cleanupUploadedFile(info);
            throw e;
        }
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
    public List<SysFile> listByBiz(String bizType, Long bizId) {
        if (StrUtil.isBlank(bizType) || bizId == null) {
            return List.of();
        }
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SYS_FILE.BIZ_TYPE.eq(bizType))
                .and(SYS_FILE.BIZ_ID.eq(bizId))
                .orderBy(SYS_FILE.UPLOAD_TIME.desc());
        return list(wrapper);
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
        int success = 0;
        for (Long id : ids) {
            SysFile entity = getById(id);
            if (entity == null) {
                continue;
            }
            boolean storageDeleted = fileStorageService.delete(entity.getFilePath());
            if (!storageDeleted) {
                throw new BizException("文件存储删除失败: " + entity.getOriginalName());
            }
            removeById(id);
            success++;
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByPath(String path) {
        if (StrUtil.isBlank(path)) {
            return false;
        }
        List<Long> ids = list(QueryWrapper.create()
                .where(SYS_FILE.FILE_PATH.eq(path.trim())))
                .stream()
                .map(SysFile::getId)
                .filter(Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return false;
        }
        return deleteByIds(ids) > 0;
    }

    @Override
    public boolean existsByPath(String path) {
        if (StrUtil.isBlank(path)) {
            return false;
        }
        return list(QueryWrapper.create()
                .where(SYS_FILE.FILE_PATH.eq(path.trim())))
                .stream()
                .findFirst()
                .map(entity -> fileStorageService.exists(entity.getFilePath()))
                .orElse(false);
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
