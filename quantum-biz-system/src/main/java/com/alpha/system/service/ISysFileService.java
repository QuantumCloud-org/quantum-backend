package com.alpha.system.service;

import com.alpha.file.entity.FileInfo;
import com.alpha.system.domain.SysFile;
import com.alpha.system.dto.request.SysFileQuery;
import com.mybatisflex.core.paginate.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * 文件管理服务接口
 */
public interface ISysFileService {

    /** 允许挂载的业务类型白名单 */
    Set<String> ALLOWED_BIZ_TYPES = Set.of(
            "other",
            "avatar",
            "attachment",
            "talent_attachment",
            "import",
            "export",
            "icon"
    );

    /**
     * 上传文件并落库
     */
    SysFile upload(MultipartFile file, String bizType, Long bizId, String path);

    /**
     * 批量上传
     */
    List<SysFile> uploadBatch(List<MultipartFile> files, String bizType, Long bizId, String path);

    /**
     * 按 id 查询
     */
    SysFile selectById(Long id);

    /**
     * 分页查询
     */
    Page<SysFile> selectFilePage(SysFileQuery query);

    /**
     * 按业务对象查询
     */
    List<SysFile> listByBiz(String bizType, Long bizId);

    /**
     * 按 id 下载（返回流）
     */
    InputStream downloadById(Long id);

    /**
     * 按 id 批量删除（同时删存储和数据库）
     */
    int deleteByIds(List<Long> ids);

    /**
     * 按已登记文件路径删除（兼容接口）
     */
    boolean deleteByPath(String path);

    /**
     * 按已登记文件路径检查文件是否存在
     */
    boolean existsByPath(String path);

    /**
     * 把已有 FileInfo 落库（供其他业务模块上传后登记元数据）
     */
    SysFile saveFromFileInfo(FileInfo info, String bizType, Long bizId);
}
