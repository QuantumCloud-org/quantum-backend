package com.alpha.file.config;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Excel 导出配置
 *
 * @author alpha
 */
@Data
@Builder
public class ExcelExportConfig {

    /**
     * 是否导出表头（默认 true）
     */
    @Builder.Default
    private boolean useHeader = true;

    /**
     * 表头行数（默认 1），用于多级表头
     */
    @Builder.Default
    private int headRowNumber = 1;

    /**
     * Sheet 名称
     */
    private String sheetName;

    /**
     * 是否自动列宽（默认 true）
     */
    @Builder.Default
    private boolean autoColumnWidth = true;

    /**
     * 冻结首行（默认 true）
     */
    @Builder.Default
    private boolean freezeFirstRow = true;

    /**
     * 导出模板模式（只导表头，无数据）
     */
    @Builder.Default
    private boolean templateMode = false;

    /**
     * 合计行配置
     */
    private SummaryConfig summary;

    /**
     * 自定义列宽（列索引 -> 宽度）
     */
    private Map<Integer, Integer> customColumnWidth;

    /**
     * 数值列索引列表（用于合计计算）
     */
    private List<Integer> numericColumns;

    /**
     * 合计行配置
     */
    @Data
    @Builder
    public static class SummaryConfig {
        /**
         * 是否启用合计行（默认 false）
         */
        @Builder.Default
        private boolean enabled = false;

        /**
         * 合计行位置：top / bottom（默认 bottom）
         */
        @Builder.Default
        private String position = "bottom";

        /**
         * 合计行标签（默认 "合计"）
         */
        @Builder.Default
        private String label = "合计";

        /**
         * 合计列索引列表（-1 表示第一列）
         */
        private List<Integer> columns;

        /**
         * 合计方法：sum / avg / count / max / min
         */
        private String method;
    }
}
