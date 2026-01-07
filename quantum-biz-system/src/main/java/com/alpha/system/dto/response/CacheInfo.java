package com.alpha.system.dto.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 缓存信息
 */
@Data
public class CacheInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 缓存名称
     */
    private String name;

    /**
     * 命中次数
     */
    private long hitCount;

    /**
     * 未命中次数
     */
    private long missCount;

    /**
     * 命中率
     */
    private String hitRate;

    /**
     * 缓存大小
     */
    private long size;

    /**
     * 驱逐次数
     */
    private long evictionCount;

    /**
     * 加载次数
     */
    private long loadCount;
}
