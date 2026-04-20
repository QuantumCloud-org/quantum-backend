package com.alpha.orm.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public abstract class BaseEntity implements Serializable {

    /**
     * 主键 ID
     * <p>
     * 使用雪花算法生成，确保分布式环境下的唯一性
     */
    @Id(keyType = KeyType.Generator, value=KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 创建时间
     * <p>
     * 由 MyBatis-Flex 审计功能自动填充
     */
    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    /**
     * 创建人 ID
     * <p>
     * 由 AuditInterceptor 自动填充当前登录用户 ID
     */
    private Long createBy;

    /**
     * 更新时间
     * <p>
     * 由 MyBatis-Flex 审计功能自动填充
     */
    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;

    /**
     * 更新人 ID
     * <p>
     * 由 AuditInterceptor 自动填充当前登录用户 ID
     */
    private Long updateBy;

    /**
     * 逻辑删除标识
     * <p>
     * 0-未删除，1-已删除
     * 数据库字段名为 deleted
     */
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;

    /**
     * 乐观锁版本号
     * <p>
     * 用于并发控制，每次更新自动 +1
     */
    @Column(version = true)
    private Long version;

}
