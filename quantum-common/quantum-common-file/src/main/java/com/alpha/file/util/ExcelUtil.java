package com.alpha.file.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.idev.excel.FastExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import cn.idev.excel.enums.CellExtraTypeEnum;
import cn.idev.excel.metadata.CellExtra;
import cn.idev.excel.read.listener.ReadListener;
import cn.idev.excel.support.ExcelTypeEnum;
import cn.idev.excel.write.builder.ExcelWriterBuilder;
import cn.idev.excel.write.builder.ExcelWriterSheetBuilder;
import cn.idev.excel.write.handler.SheetWriteHandler;
import cn.idev.excel.write.merge.LoopMergeStrategy;
import cn.idev.excel.write.merge.OnceAbsoluteMergeStrategy;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.idev.excel.write.metadata.holder.WriteSheetHolder;
import cn.idev.excel.write.metadata.holder.WriteWorkbookHolder;
import cn.idev.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.alpha.file.config.ExcelExportConfig;
import com.alpha.file.listener.BatchDataListener;
import com.alpha.framework.exception.BizException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/**
 * Excel 工具类
 * <p>
 * 基于 FastExcel 封装，支持：
 * - 合并单元格读取/写入
 * - 跳过首X行导入
 * - 导出是否包含表头
 * - 模板模式、合计行、自定义列宽等
 */
@Slf4j
public class ExcelUtil {

    private ExcelUtil() {
    }

    // ==================== 读取配置类 ====================

    @Data
    @Builder
    public static class ReadConfig {
        @Builder.Default
        private Integer headRowNumber = 1;
        @Builder.Default
        private Integer sheetNo = 0;
        private String sheetName;
        @Builder.Default
        private Boolean handleMergedCell = false;
        @Builder.Default
        private ExcelTypeEnum excelType = ExcelTypeEnum.XLSX;
    }

    @Data
    @Builder
    public static class MergeConfig {
        @Builder.Default
        private MergeType type = MergeType.ONCE;
        private Integer firstRow;
        private Integer lastRow;
        private Integer firstCol;
        private Integer lastCol;
        private Integer eachRow;
        private Integer columnIndex;

        public static MergeConfig once(int firstRow, int lastRow, int firstCol, int lastCol) {
            return MergeConfig.builder().type(MergeType.ONCE)
                    .firstRow(firstRow).lastRow(lastRow)
                    .firstCol(firstCol).lastCol(lastCol).build();
        }

        public static MergeConfig loop(int eachRow, int columnIndex) {
            return MergeConfig.builder().type(MergeType.LOOP)
                    .eachRow(eachRow).columnIndex(columnIndex).build();
        }
    }

    public enum MergeType { ONCE, LOOP }

    // ==================== 导出方法 ====================

    public static <T> void export(HttpServletResponse response, String fileName,
                                  Class<T> clazz, List<T> data) {
        export(response, fileName, clazz, data, "Sheet1");
    }

    public static <T> void export(HttpServletResponse response, String fileName,
                                  Class<T> clazz, List<T> data, String sheetName) {
        try {
            setExportResponse(response, fileName);
            FastExcel.write(response.getOutputStream(), clazz)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet(sheetName)
                    .doWrite(data);
        } catch (IOException e) {
            log.error("Excel 导出失败", e);
            throw new BizException("Excel 导出失败: " + e.getMessage());
        }
    }

    public static <T> void export(HttpServletResponse response, String fileName,
                                  Class<T> clazz, List<T> data, ExcelExportConfig config) {
        export(response, fileName, clazz, data, config, null);
    }

    public static <T> void export(HttpServletResponse response, String fileName,
                                  Class<T> clazz, List<T> data,
                                  ExcelExportConfig config, List<MergeConfig> mergeConfigs) {
        try {
            setExportResponse(response, fileName);
            doWrite(response.getOutputStream(), clazz, data, config, mergeConfigs);
        } catch (IOException e) {
            log.error("Excel 导出失败", e);
            throw new BizException("Excel 导出失败: " + e.getMessage());
        }
    }

    public static <T> void exportWithoutHead(HttpServletResponse response, String fileName,
                                             Class<T> clazz, List<T> data) {
        export(response, fileName, clazz, data,
                ExcelExportConfig.builder().useHeader(false).build());
    }

    public static <T> void exportTemplate(HttpServletResponse response, String fileName, Class<T> clazz) {
        export(response, fileName, clazz, Collections.emptyList(),
                ExcelExportConfig.builder().templateMode(true).build());
    }

    public static <T> void exportWithMerge(HttpServletResponse response, String fileName,
                                           Class<T> clazz, List<T> data, List<MergeConfig> mergeConfigs) {
        export(response, fileName, clazz, data, ExcelExportConfig.builder().build(), mergeConfigs);
    }

    public static <T> void exportWithSummary(HttpServletResponse response, String fileName,
                                             Class<T> clazz, List<T> data,
                                             List<Integer> columns, String method) {
        ExcelExportConfig config = ExcelExportConfig.builder()
                .summary(ExcelExportConfig.SummaryConfig.builder()
                        .enabled(true).columns(columns).method(method).build())
                .build();
        export(response, fileName, clazz, data, config);
    }

    public static <T> void exportDynamicHead(HttpServletResponse response, String fileName,
                                             List<List<String>> head, List<T> data) {
        exportDynamicHead(response, fileName, head, data, ExcelExportConfig.builder().build());
    }

    public static <T> void exportDynamicHead(HttpServletResponse response, String fileName,
                                             List<List<String>> head, List<T> data,
                                             ExcelExportConfig config) {
        try {
            setExportResponse(response, fileName);
            ExcelWriterBuilder builder = FastExcel.write(response.getOutputStream())
                    .head(head)
                    .needHead(config.isUseHeader())
                    .registerConverter(new LongStringConverter());
            applyConfig(builder, config, null);
            builder.sheet(getSheetName(config)).doWrite(data);
        } catch (IOException e) {
            log.error("Excel 导出失败", e);
            throw new BizException("Excel 导出失败: " + e.getMessage());
        }
    }

    public static <T> void export(OutputStream outputStream, Class<T> clazz, List<T> data, String sheetName) {
        FastExcel.write(outputStream, clazz)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet(sheetName)
                .doWrite(data);
    }

    public static void exportMultiSheet(HttpServletResponse response, String fileName, List<SheetData<?>> sheets) {
        exportMultiSheet(response, fileName, sheets, ExcelExportConfig.builder().build());
    }

    public static void exportMultiSheet(HttpServletResponse response, String fileName,
                                        List<SheetData<?>> sheets, ExcelExportConfig config) {
        try {
            setExportResponse(response, fileName);
            try (ExcelWriter writer = FastExcel.write(response.getOutputStream()).build()) {
                for (int i = 0; i < sheets.size(); i++) {
                    SheetData<?> sheet = sheets.get(i);
                    ExcelWriterSheetBuilder sheetBuilder = FastExcel.writerSheet(i, sheet.sheetName())
                            .head(sheet.clazz())
                            .needHead(config.isUseHeader());
                    if (config.isAutoColumnWidth()) {
                        sheetBuilder.registerWriteHandler(new LongestMatchColumnWidthStyleStrategy());
                    }
                    if (config.isFreezeFirstRow()) {
                        sheetBuilder.registerWriteHandler(new FreezeRowHandler());
                    }
                    writer.write(sheet.data(), sheetBuilder.build());
                }
            }
        } catch (IOException e) {
            log.error("Excel 导出失败", e);
            throw new BizException("Excel 导出失败: " + e.getMessage());
        }
    }

    // ==================== 导入方法 ====================

    public static <T> List<T> read(MultipartFile file, Class<T> clazz) {
        try {
            return read(file.getInputStream(), clazz);
        } catch (IOException e) {
            log.error("Excel 读取失败", e);
            throw new BizException("Excel 读取失败: " + e.getMessage());
        }
    }

    public static <T> List<T> read(MultipartFile file, Class<T> clazz, int sheetNo) {
        try {
            return FastExcel.read(file.getInputStream()).head(clazz).sheet(sheetNo).doReadSync();
        } catch (IOException e) {
            log.error("Excel 读取失败", e);
            throw new BizException("Excel 读取失败: " + e.getMessage());
        }
    }

    public static <T> List<T> read(InputStream inputStream, Class<T> clazz) {
        return FastExcel.read(inputStream).head(clazz).sheet().doReadSync();
    }

    public static <T> List<T> read(InputStream inputStream, Class<T> clazz, int headRowNumber) {
        return read(inputStream, clazz, ReadConfig.builder().headRowNumber(headRowNumber).build());
    }

    public static <T> List<T> read(InputStream inputStream, Class<T> clazz, ReadConfig config) {
        if (Boolean.TRUE.equals(config.getHandleMergedCell())) {
            return readWithMerge(inputStream, clazz, config);
        }
        var builder = FastExcel.read(inputStream).head(clazz)
                .headRowNumber(config.getHeadRowNumber())
                .excelType(config.getExcelType());
        return (config.getSheetName() != null ? builder.sheet(config.getSheetName()) : builder.sheet(config.getSheetNo()))
                .doReadSync();
    }

    public static <T> List<T> readWithMerge(InputStream inputStream, Class<T> clazz, int headRowNumber) {
        return readWithMerge(inputStream, clazz, ReadConfig.builder().headRowNumber(headRowNumber).build());
    }

    public static <T> List<T> readWithMerge(InputStream inputStream, Class<T> clazz, ReadConfig config) {
        MergedCellReadListener<T> listener = new MergedCellReadListener<>();
        FastExcel.read(inputStream, clazz, listener)
                .headRowNumber(config.getHeadRowNumber())
                .excelType(config.getExcelType())
                .extraRead(CellExtraTypeEnum.MERGE)
                .sheet(config.getSheetNo())
                .doRead();
        return listener.getDataList();
    }

    public static <T> void read(MultipartFile file, Class<T> clazz, ReadListener<T> listener) {
        try {
            FastExcel.read(file.getInputStream(), clazz, listener).sheet().doRead();
        } catch (IOException e) {
            log.error("Excel 读取失败", e);
            throw new BizException("Excel 读取失败: " + e.getMessage());
        }
    }

    public static <T> void readBatch(InputStream inputStream, Class<T> clazz,
                                     int batchSize, Consumer<List<T>> consumer) {
        readBatch(inputStream, clazz, batchSize, consumer, ReadConfig.builder().build());
    }

    public static <T> void readBatch(InputStream inputStream, Class<T> clazz,
                                     int batchSize, Consumer<List<T>> consumer, ReadConfig config) {
        BatchDataListener<T> listener = new BatchDataListener<>(batchSize, consumer);
        FastExcel.read(inputStream, clazz, listener)
                .headRowNumber(config.getHeadRowNumber())
                .excelType(config.getExcelType())
                .sheet(config.getSheetNo())
                .doRead();
    }

    public static <T> ReadResult<T> readWithValidation(InputStream inputStream, Class<T> clazz, ReadConfig config) {
        ValidationReadListener<T> listener = new ValidationReadListener<>();
        FastExcel.read(inputStream, clazz, listener)
                .headRowNumber(config.getHeadRowNumber())
                .excelType(config.getExcelType())
                .sheet(config.getSheetNo())
                .doRead();
        return new ReadResult<>(listener.getDataList(), listener.getErrors());
    }

    // ==================== 内部实现 ====================

    private static <T> void doWrite(OutputStream os, Class<T> clazz, List<T> data,
                                    ExcelExportConfig config, List<MergeConfig> merges) {
        List<T> writeData = config.isTemplateMode() ? Collections.emptyList() : data;
        List<List<Object>> summary = null;
        if (config.getSummary() != null && config.getSummary().isEnabled() && !writeData.isEmpty()) {
            summary = calculateSummary(writeData, config);
        }

        ExcelWriterBuilder builder = FastExcel.write(os, clazz)
                .needHead(config.isUseHeader())
                .registerConverter(new LongStringConverter());
        applyConfig(builder, config, merges);

        ExcelWriter writer = builder.build();
        WriteSheet sheet = FastExcel.writerSheet(getSheetName(config)).build();

        if (summary != null && "top".equalsIgnoreCase(config.getSummary().getPosition())) {
            writer.write(summary, sheet);
        }
        writer.write(writeData, sheet);
        if (summary != null && !"top".equalsIgnoreCase(config.getSummary().getPosition())) {
            writer.write(summary, sheet);
        }
        writer.finish();
    }

    private static void applyConfig(ExcelWriterBuilder builder, ExcelExportConfig config, List<MergeConfig> merges) {
        if (config.isAutoColumnWidth()) {
            builder.registerWriteHandler(new LongestMatchColumnWidthStyleStrategy());
        }
        if (CollectionUtil.isNotEmpty(config.getCustomColumnWidth())) {
            builder.registerWriteHandler(new CustomColumnWidthHandler(config.getCustomColumnWidth()));
        }
        if (config.isFreezeFirstRow()) {
            builder.registerWriteHandler(new FreezeRowHandler());
        }
        if (CollectionUtil.isNotEmpty(merges)) {
            for (MergeConfig mc : merges) {
                if (mc.getType() == MergeType.ONCE) {
                    builder.registerWriteHandler(new OnceAbsoluteMergeStrategy(
                            mc.getFirstRow(), mc.getLastRow(), mc.getFirstCol(), mc.getLastCol()));
                } else {
                    builder.registerWriteHandler(new LoopMergeStrategy(mc.getEachRow(), mc.getColumnIndex()));
                }
            }
        }
    }

    private static <T> List<List<Object>> calculateSummary(List<T> data, ExcelExportConfig config) {
        var sc = config.getSummary();
        if (CollectionUtil.isEmpty(sc.getColumns()) || data.isEmpty()) return null;

        Class<?> clazz = data.get(0).getClass();
        Field[] fields = getExcelFields(clazz);
        Map<Integer, List<BigDecimal>> colValues = new HashMap<>();

        for (T item : data) {
            for (int col : sc.getColumns()) {
                if (col >= 0 && col < fields.length) {
                    try {
                        Field f = fields[col];
                        f.setAccessible(true);
                        Object v = f.get(item);
                        if (v != null) colValues.computeIfAbsent(col, k -> new ArrayList<>()).add(new BigDecimal(v.toString()));
                    } catch (Exception ignored) {}
                }
            }
        }

        Map<Integer, BigDecimal> results = new HashMap<>();
        String method = sc.getMethod() != null ? sc.getMethod().toLowerCase() : "sum";
        for (var e : colValues.entrySet()) {
            List<BigDecimal> vals = e.getValue();
            BigDecimal r = switch (method) {
                case "avg" -> vals.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(vals.size()), 2, RoundingMode.HALF_UP);
                case "count" -> BigDecimal.valueOf(vals.size());
                case "max" -> vals.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                case "min" -> vals.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                default -> vals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            };
            results.put(e.getKey(), r);
        }

        List<Object> row = new ArrayList<>();
        for (int i = 0; i < fields.length; i++) {
            if (i == 0) row.add(sc.getLabel());
            else if (results.containsKey(i)) row.add(results.get(i));
            else row.add("");
        }
        return Collections.singletonList(row);
    }

    private static Field[] getExcelFields(Class<?> clazz) {
        List<Field> result = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(ExcelIgnore.class)) continue;
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) continue;
            result.add(f);
        }
        return result.toArray(new Field[0]);
    }

    private static String getSheetName(ExcelExportConfig config) {
        return config.getSheetName() != null ? config.getSheetName() : "Sheet1";
    }

    private static void setExportResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + encoded + ".xlsx");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
    }

    // ==================== Handler ====================

    public static class FreezeRowHandler implements SheetWriteHandler {
        @Override
        public void afterSheetCreate(WriteWorkbookHolder h, WriteSheetHolder sh) {
            sh.getSheet().createFreezePane(0, 1);
        }
    }

    public static class CustomColumnWidthHandler implements SheetWriteHandler {
        private final Map<Integer, Integer> widthMap;
        public CustomColumnWidthHandler(Map<Integer, Integer> widthMap) { this.widthMap = widthMap; }
        @Override
        public void afterSheetCreate(WriteWorkbookHolder h, WriteSheetHolder sh) {
            Sheet sheet = sh.getSheet();
            widthMap.forEach((col, width) -> sheet.setColumnWidth(col, width * 256));
        }
    }

    // ==================== 监听器 ====================

    public static class MergedCellReadListener<T> implements ReadListener<T> {
        @Getter private final List<T> dataList = new ArrayList<>();
        private final List<CellExtra> mergeList = new ArrayList<>();
        private Integer headRowNumber;

        @Override public void invoke(T data, AnalysisContext ctx) {
            if (headRowNumber == null) headRowNumber = ctx.readSheetHolder().getHeadRowNumber();
            dataList.add(data);
        }
        @Override public void extra(CellExtra extra, AnalysisContext ctx) {
            if (extra.getType() == CellExtraTypeEnum.MERGE) mergeList.add(extra);
        }
        @Override public void doAfterAllAnalysed(AnalysisContext ctx) {
            if (!mergeList.isEmpty()) processMergedCells();
        }

        private void processMergedCells() {
            if (dataList.isEmpty()) return;
            Class<?> clazz = dataList.get(0).getClass();
            for (CellExtra m : mergeList) {
                int fr = m.getFirstRowIndex() - headRowNumber, lr = m.getLastRowIndex() - headRowNumber;
                int fc = m.getFirstColumnIndex(), lc = m.getLastColumnIndex();
                if (fr < 0 || fr >= dataList.size()) continue;
                try {
                    Field f = getFieldByIndex(clazz, fc);
                    if (f == null) continue;
                    f.setAccessible(true);
                    Object val = f.get(dataList.get(fr));
                    if (val == null) continue;
                    for (int r = fr; r <= lr && r < dataList.size(); r++) {
                        for (int c = fc; c <= lc; c++) {
                            if (r == fr && c == fc) continue;
                            Field tf = getFieldByIndex(clazz, c);
                            if (tf != null) { tf.setAccessible(true); tf.set(dataList.get(r), convert(val, tf.getType())); }
                        }
                    }
                } catch (Exception e) { log.warn("处理合并单元格失败", e); }
            }
        }

        private Field getFieldByIndex(Class<?> clazz, int idx) {
            for (Field f : clazz.getDeclaredFields()) {
                ExcelProperty p = f.getAnnotation(ExcelProperty.class);
                if (p != null && p.index() == idx) return f;
            }
            int i = 0;
            for (Field f : clazz.getDeclaredFields()) {
                if (f.isAnnotationPresent(ExcelIgnore.class) || Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) continue;
                if (i == idx) return f;
                i++;
            }
            return null;
        }

        private Object convert(Object v, Class<?> t) {
            if (v == null || t.isAssignableFrom(v.getClass())) return v;
            String s = v.toString();
            if (t == String.class) return s;
            if (t == Integer.class || t == int.class) return Double.valueOf(s).intValue();
            if (t == Long.class || t == long.class) return Double.valueOf(s).longValue();
            if (t == Double.class || t == double.class) return Double.parseDouble(s);
            if (t == BigDecimal.class) return new BigDecimal(s);
            return v;
        }
    }

    public static class ValidationReadListener<T> implements ReadListener<T> {
        @Getter private final List<T> dataList = new ArrayList<>();
        @Getter private final List<ReadError> errors = new ArrayList<>();
        @Override public void invoke(T data, AnalysisContext ctx) { dataList.add(data); }
        @Override public void onException(Exception e, AnalysisContext ctx) { errors.add(new ReadError(ctx.readRowHolder().getRowIndex(), e.getMessage())); }
        @Override public void doAfterAllAnalysed(AnalysisContext ctx) {}
    }

    // ==================== 数据类 ====================

    public record ReadResult<T>(List<T> data, List<ReadError> errors) {
        public boolean hasErrors() { return !errors.isEmpty(); }
    }
    public record ReadError(int rowIndex, String message) {}

    public record SheetData<T>(String sheetName, Class<T> clazz, List<T> data) {}

}