package com.alpha.file.util;

import cn.idev.excel.FastExcel;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.exception.ExcelAnalysisStopException;
import cn.idev.excel.exception.ExcelDataConvertException;
import cn.idev.excel.read.listener.ReadListener;
import cn.idev.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.alpha.framework.exception.BizException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class ExcelUtil {

    private ExcelUtil() {
    }

    /** A worksheet row with its one-based position and any cell conversion error. */
    public record ReadRow<T>(int number, T data, String error) {}

    public static <T> List<ReadRow<T>> read(InputStream inputStream, Class<T> clazz, int maxRows) {
        List<ReadRow<T>> rows = new ArrayList<>();
        FastExcel.read(inputStream, clazz, new ReadListener<T>() {
            @Override
            public void invoke(T row, AnalysisContext context) {
                rows.add(new ReadRow<>(context.readRowHolder().getRowIndex() + 1, row, null));
            }

            @Override
            public void onException(Exception error, AnalysisContext context) throws Exception {
                if (!(error instanceof ExcelDataConvertException cell)) {
                    throw error;
                }
                rows.add(new ReadRow<>(cell.getRowIndex() + 1, null,
                        "第 " + (cell.getColumnIndex() + 1) + " 列单元格格式不正确"));
                if (rows.size() > maxRows) {
                    throw new ExcelAnalysisStopException();
                }
            }

            @Override
            public boolean hasNext(AnalysisContext context) {
                return rows.size() <= maxRows;
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
            }
        }).sheet().doRead();
        if (rows.size() > maxRows) {
            throw new BizException("导入最多 " + maxRows + " 行");
        }
        return rows;
    }

    public static <T> void export(HttpServletResponse response, String fileName, Class<T> clazz, List<T> data) {
        try {
            setExportResponse(response, fileName);
            FastExcel.write(response.getOutputStream(), clazz)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("Sheet1")
                    .doWrite(data);
        } catch (IOException e) {
            log.error("Excel 导出失败", e);
            throw new BizException("Excel 导出失败");
        }
    }

    private static void setExportResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=\"export.xlsx\"; filename*=UTF-8''" + encoded);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
    }
}
