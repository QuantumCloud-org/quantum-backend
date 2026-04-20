package com.alpha.orm.entity;

import com.mybatisflex.core.paginate.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页查询基类
 * <p>
 * 业务模块继承此类，添加业务查询参数：
 * <pre>
 * public class UserQuery extends BasePageQuery {
 *     private String username;
 *     private Integer status;
 * }
 * </pre>
 */
@Data
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    @Min(value = 1, message = "页码最小为 1")
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 2000, message = "每页条数最大为 2000")
    private Integer pageSize = 10;

    /**
     * 排序字段（如：createTime 或 create_time）
     */
    private String orderBy;

    /**
     * 排序方向：asc / desc
     */
    private String orderDirection;

    /**
     * 转换为 MyBatis-Flex Page
     */
    public <T> Page<T> getPage() {
        return Page.of(pageNum, pageSize);
    }

    /**
     * 是否升序
     */
    public boolean isAsc() {
        return "asc".equalsIgnoreCase(orderDirection);
    }
}
