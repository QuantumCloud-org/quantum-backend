package com.alpha.system.dto.response;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * 服务器信息
 */
@Data
public class ServerInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * CPU 信息
     */
    private Cpu cpu = new Cpu();

    /**
     * 内存信息
     */
    private Mem mem = new Mem();

    /**
     * JVM 信息
     */
    private Jvm jvm = new Jvm();

    /**
     * 系统信息
     */
    private Sys sys = new Sys();

    /**
     * 磁盘信息
     */
    private List<SysFile> sysFiles = new ArrayList<>();

    /**
     * 收集服务器信息
     */
    public void collect() {
        // 收集 JVM 信息
        collectJvmInfo();
        // 收集内存信息
        collectMemInfo();
        // 收集系统信息
        collectSysInfo();
        // 收集 CPU 信息
        collectCpuInfo();
        // 收集磁盘信息
        collectDiskInfo();
    }

    private void collectJvmInfo() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        Properties props = System.getProperties();

        jvm.setName(runtimeMXBean.getVmName());
        jvm.setVersion(props.getProperty("java.version"));
        jvm.setHome(props.getProperty("java.home"));
        jvm.setStartTime(DateUtil.formatDateTime(new Date(runtimeMXBean.getStartTime())));
        jvm.setRunTime(formatRunTime(runtimeMXBean.getUptime()));

        // JVM 内存
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapCommitted = memoryMXBean.getHeapMemoryUsage().getCommitted();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();

        jvm.setTotal(formatByte(heapCommitted));
        jvm.setMax(formatByte(heapMax));
        jvm.setUsed(formatByte(heapUsed));
        jvm.setFree(formatByte(Math.max(heapMax - heapUsed, 0)));
        jvm.setUsage(heapMax > 0
                ? NumberUtil.round((double) heapUsed / heapMax * 100, 2).doubleValue()
                : 0D);
    }

    private void collectMemInfo() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();

        mem.setTotal(formatByte(maxMemory));
        mem.setUsed(formatByte(totalMemory - freeMemory));
        mem.setFree(formatByte(freeMemory));
        mem.setUsage(NumberUtil.round((double) (totalMemory - freeMemory) / maxMemory * 100, 2).doubleValue());
    }

    private void collectSysInfo() {
        Properties props = System.getProperties();

        try {
            InetAddress addr = InetAddress.getLocalHost();
            sys.setComputerName(addr.getHostName());
            sys.setComputerIp(addr.getHostAddress());
        } catch (Exception e) {
            sys.setComputerName("Unknown");
            sys.setComputerIp("Unknown");
        }

        sys.setOsName(props.getProperty("os.name"));
        sys.setOsArch(props.getProperty("os.arch"));
        sys.setUserDir(props.getProperty("user.dir"));
    }

    private void collectCpuInfo() {
        int processors = Runtime.getRuntime().availableProcessors();
        cpu.setCpuNum(processors);

        // 简化的 CPU 使用率（实际应该使用 OperatingSystemMXBean）
        com.sun.management.OperatingSystemMXBean osBean =
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        double systemLoad = osBean.getCpuLoad() * 100;
        double processLoad = osBean.getProcessCpuLoad() * 100;

        cpu.setTotal(NumberUtil.round(systemLoad, 2).doubleValue());
        cpu.setSys(NumberUtil.round(systemLoad * 0.3, 2).doubleValue()); // 估算
        cpu.setUsed(NumberUtil.round(processLoad, 2).doubleValue());
        cpu.setWait(0.0);
        cpu.setFree(NumberUtil.round(100 - systemLoad, 2).doubleValue());
    }

    private void collectDiskInfo() {
        java.io.File[] roots = java.io.File.listRoots();

        for (java.io.File file : roots) {
            long total = file.getTotalSpace();
            long free = file.getFreeSpace();
            long used = total - free;

            SysFile sysFile = new SysFile();
            sysFile.setDirName(file.getPath());
            sysFile.setSysTypeName(file.getAbsolutePath());
            sysFile.setTypeName("本地磁盘");
            sysFile.setTotal(formatByte(total));
            sysFile.setFree(formatByte(free));
            sysFile.setUsed(formatByte(used));
            sysFile.setUsage(total > 0 ? NumberUtil.round((double) used / total * 100, 2).doubleValue() : 0);

            sysFiles.add(sysFile);
        }
    }

    /**
     * 格式化字节
     */
    private String formatByte(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return NumberUtil.round((double) bytes / 1024, 2) + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return NumberUtil.round((double) bytes / (1024 * 1024), 2) + " MB";
        } else {
            return NumberUtil.round((double) bytes / (1024 * 1024 * 1024), 2) + " GB";
        }
    }

    /**
     * 格式化运行时间
     */
    private String formatRunTime(long millis) {
        long days = millis / (24 * 60 * 60 * 1000);
        long hours = (millis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (millis % (60 * 60 * 1000)) / (60 * 1000);

        return days + "天" + hours + "小时" + minutes + "分钟";
    }

    // ==================== 内部类 ====================

    @Data
    public static class Cpu implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 核心数 */
        private int cpuNum;
        /** 总使用率 */
        private double total;
        /** 系统使用率 */
        private double sys;
        /** 用户使用率 */
        private double used;
        /** 等待率 */
        private double wait;
        /** 空闲率 */
        private double free;
    }

    @Data
    public static class Mem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 总内存 */
        private String total;
        /** 已用内存 */
        private String used;
        /** 剩余内存 */
        private String free;
        /** 使用率 */
        private double usage;
    }

    @Data
    public static class Jvm implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** JVM 名称 */
        private String name;
        /** 版本 */
        private String version;
        /** JAVA_HOME */
        private String home;
        /** 总内存 */
        private String total;
        /** 最大可用内存 */
        private String max;
        /** 已用内存 */
        private String used;
        /** 空闲内存 */
        private String free;
        /** 使用率 */
        private double usage;
        /** 启动时间 */
        private String startTime;
        /** 运行时长 */
        private String runTime;
    }

    @Data
    public static class Sys implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 计算机名 */
        private String computerName;
        /** 计算机IP */
        private String computerIp;
        /** 操作系统 */
        private String osName;
        /** 系统架构 */
        private String osArch;
        /** 项目路径 */
        private String userDir;
    }

    @Data
    public static class SysFile implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 盘符路径 */
        private String dirName;
        /** 盘符类型 */
        private String sysTypeName;
        /** 文件类型 */
        private String typeName;
        /** 总大小 */
        private String total;
        /** 剩余大小 */
        private String free;
        /** 已用大小 */
        private String used;
        /** 使用率 */
        private double usage;
    }
}
