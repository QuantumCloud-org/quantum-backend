package com.alpha.orm.entity;

import com.mybatisflex.core.paginate.Page;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 分页结果
 * <p>
 * 统一分页返回格式，与前端约定字段名
 *
 * @param <T> 数据类型
 */
@Data
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    private long pageNum;

    /**
     * 每页条数
     */
    private long pageSize;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 总页数
     */
    private long pages;

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 从 MyBatis-Flex Page 转换（直接使用实体）
     */
    public static <T> PageResult<T> of(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(page.getPageNumber());
        result.setPageSize(page.getPageSize());
        result.setTotal(page.getTotalRow());
        result.setPages(page.getTotalPage());
        result.setRecords(page.getRecords() != null ? page.getRecords() : Collections.emptyList());
        return result;
    }

    /**
     * 从 MyBatis-Flex Page 转换（带类型转换，Entity -> VO）
     * <p>
     * 使用示例：
     * <pre>
     * // 方法引用
     * PageResult.of(page, UserVO::from);
     *
     * // Lambda
     * PageResult.of(page, user -> userConverter.toVO(user));
     *
     * // MapStruct
     * PageResult.of(page, userMapper::toVO);
     * </pre>
     */
    public static <S, T> PageResult<T> of(Page<S> page, Function<S, T> converter) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(page.getPageNumber());
        result.setPageSize(page.getPageSize());
        result.setTotal(page.getTotalRow());
        result.setPages(page.getTotalPage());

        if (page.getRecords() != null && !page.getRecords().isEmpty()) {
            result.setRecords(page.getRecords().stream().map(converter).toList());
        } else {
            result.setRecords(Collections.emptyList());
        }
        return result;
    }

    /**
     * 空分页结果
     */
    public static <T> PageResult<T> empty() {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(1);
        result.setPageSize(10);
        result.setTotal(0);
        result.setPages(0);
        result.setRecords(Collections.emptyList());
        return result;
    }

    /**
     * 手动构建分页结果（用于非 MyBatis-Flex 场景）
     */
    public static <T> PageResult<T> of(List<T> records, long total, int pageNum, int pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setPages(pageSize > 0 ? (total + pageSize - 1) / pageSize : 0);
        result.setRecords(records != null ? records : Collections.emptyList());
        return result;
    }

}