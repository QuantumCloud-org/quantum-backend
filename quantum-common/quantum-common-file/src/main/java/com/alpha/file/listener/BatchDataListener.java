package com.alpha.file.listener;

import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.read.listener.ReadListener;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Excel 批量数据监听器
 * <p>
 * 适用于大数据量导入场景，分批处理数据
 * <p>
 * 使用示例：
 * <pre>
 * ExcelUtil.read(file, UserImportVO.class, new BatchDataListener&lt;&gt;(
 *     100,  // 每批 100 条
 *     batch -&gt; userService.saveBatch(batch)
 * ));
 * </pre>
 *
 * @param <T> 数据类型
 */
@Slf4j
public class BatchDataListener<T> implements ReadListener<T> {

    /**
     * 默认批量大小
     */
    private static final int DEFAULT_BATCH_SIZE = 100;

    /**
     * 批量大小
     */
    private final int batchSize;

    /**
     * 批量处理函数
     */
    private final Consumer<List<T>> consumer;

    /**
     * 数据缓冲区
     */
    private List<T> dataList;

    /**
     * 总处理数量
     */
    private int totalCount = 0;

    /**
     * 构造函数（默认批量大小）
     *
     * @param consumer 批量处理函数
     */
    public BatchDataListener(Consumer<List<T>> consumer) {
        this(DEFAULT_BATCH_SIZE, consumer);
    }

    /**
     * 构造函数
     *
     * @param batchSize 批量大小
     * @param consumer  批量处理函数
     */
    public BatchDataListener(int batchSize, Consumer<List<T>> consumer) {
        this.batchSize = batchSize;
        this.consumer = consumer;
        this.dataList = new ArrayList<>(batchSize);
    }

    @Override
    public void invoke(T data, AnalysisContext context) {
        dataList.add(data);
        if (dataList.size() >= batchSize) {
            processBatch();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 处理剩余数据
        if (!dataList.isEmpty()) {
            processBatch();
        }
        log.info("Excel 导入完成，共处理 {} 条数据", totalCount);
    }

    /**
     * 处理一批数据
     */
    private void processBatch() {
        int size = dataList.size();
        try {
            consumer.accept(dataList);
            totalCount += size;
            log.debug("批量处理完成，本批 {} 条，累计 {} 条", size, totalCount);
        } catch (Exception e) {
            log.error("批量处理失败，本批 {} 条数据丢失", size, e);
            throw e;
        } finally {
            // 清空缓冲区
            dataList = new ArrayList<>(batchSize);
        }
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        log.error("Excel 解析异常，行号: {}, 错误: {}",
                context.readRowHolder().getRowIndex(),
                exception.getMessage());
        throw exception;
    }

    /**
     * 获取已处理的总数量
     */
    public int getTotalCount() {
        return totalCount;
    }
}