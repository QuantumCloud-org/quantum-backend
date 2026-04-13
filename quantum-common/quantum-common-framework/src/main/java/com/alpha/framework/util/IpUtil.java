package com.alpha.framework.util;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.alpha.framework.config.TrustedProxyProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.LongByteArray;
import org.lionsoul.ip2region.xdb.Searcher;
import org.lionsoul.ip2region.xdb.Version;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * IP 工具类（基于 ip2region 离线库）
 * <p>
 * 功能：
 * 1. 获取客户端真实 IP
 * 2. 判断 IP 类型（内网/外网/IPv6）
 * 3. 获取 IP 归属地（离线查询，毫秒级响应）
 * <p>
 * 使用 ip2region v2.0 xdb 文件格式
 * 数据文件：src/main/resources/ip2region/ip2region.xdb
 */
@Slf4j
@Component
public class IpUtil {

    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IP = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    private static final String INTERNAL_IP = "内网IP";

    /**
     * IPv4 正则
     */
    private static final Pattern IPV4_PATTERN = Pattern.compile("^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
    );

    /**
     * ip2region 搜索器（线程安全）
     */
    private static Searcher searcher;

    /**
     * 缓存的 xdb 数据（完全基于内存查询）
     */
    private static byte[] xdbData;

    /**
     * 初始化 ip2region
     */
    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("ip2region/ip2region.xdb");
            if (!resource.exists()) {
                log.warn("ip2region.xdb 文件不存在，IP 归属地查询将返回 unknown");
                return;
            }

            try (InputStream is = resource.getInputStream()) {
                // 加载到内存，实现毫秒级查询
                xdbData = IoUtil.readBytes(is);
                LongByteArray buffer = new LongByteArray();
                buffer.append(xdbData);
                searcher = Searcher.newWithBuffer(Version.IPv4, buffer);
                log.info("ip2region 初始化成功，数据大小: {} KB", xdbData.length / 1024);
            }
        } catch (Exception e) {
            log.error("ip2region 初始化失败", e);
        }
    }

    /**
     * 销毁资源
     */
    @PreDestroy
    public void destroy() {
        if (searcher != null) {
            try {
                searcher.close();
            } catch (Exception ignored) {
            }
        }
    }

    // ==================== 获取客户端 IP ====================

    /**
     * 获取客户端真实 IP
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        String directRemote = normalizeLoopback(request.getRemoteAddr());

        if (isTrustedProxy(directRemote)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (StrUtil.isNotBlank(xff) && !UNKNOWN.equalsIgnoreCase(xff)) {
                String firstIp = xff.split(",")[0].trim();
                if (isValidIp(firstIp)) {
                    return normalizeLoopback(firstIp);
                }
            }
            String xri = request.getHeader("X-Real-IP");
            if (StrUtil.isNotBlank(xri) && isValidIp(xri.trim())) {
                return normalizeLoopback(xri.trim());
            }
        }

        return directRemote;
    }

    // ==================== IP 归属地查询 ====================

    /**
     * 获取 IP 归属地（离线查询）
     * <p>
     * 返回格式：国家|区域|省份|城市|ISP
     * 示例：中国|0|广东省|深圳市|电信
     *
     * @param ip IP 地址
     * @return 归属地信息，如 "广东省 深圳市"
     */
    public static String getCityInfo(String ip) {
        if (StrUtil.isBlank(ip)) {
            return UNKNOWN;
        }

        // 内网 IP
        if (isInternalIp(ip)) {
            return INTERNAL_IP;
        }

        // 搜索器未初始化
        if (searcher == null) {
            return UNKNOWN;
        }

        try {
            String region = searcher.search(ip);
            return parseRegion(region);
        } catch (Exception e) {
            log.debug("IP 归属地查询失败: {} - {}", ip, e.getMessage());
            return UNKNOWN;
        }
    }

    /**
     * 获取完整的 IP 信息
     * <p>
     * 返回原始格式：国家|区域|省份|城市|ISP
     */
    public static String getFullRegion(String ip) {
        if (StrUtil.isBlank(ip) || isInternalIp(ip) || searcher == null) {
            return UNKNOWN;
        }

        try {
            return searcher.search(ip);
        } catch (Exception e) {
            return UNKNOWN;
        }
    }

    /**
     * 解析地区信息
     * <p>
     * 输入：中国|0|广东省|深圳市|电信
     * 输出：广东省 深圳市
     */
    private static String parseRegion(String region) {
        if (StrUtil.isBlank(region)) {
            return UNKNOWN;
        }

        String[] parts = region.split("\\|");
        if (parts.length < 5) {
            return region;
        }

        String country = parts[0];  // 国家
        String province = parts[2]; // 省份
        String city = parts[3];     // 城市
        String isp = parts[4];      // 运营商

        // 去除 "0" 占位符
        province = "0".equals(province) ? "" : province;
        city = "0".equals(city) ? "" : city;

        // 国内地址
        if ("中国".equals(country)) {
            if (StrUtil.isAllNotBlank(province, city)) {
                return province + " " + city;
            } else if (StrUtil.isNotBlank(province)) {
                return province;
            }
        }

        // 国外地址
        if (StrUtil.isNotBlank(country) && !"0".equals(country)) {
            if (StrUtil.isNotBlank(city) && !"0".equals(city)) {
                return country + " " + city;
            }
            return country;
        }

        return UNKNOWN;
    }

    // ==================== IP 类型判断 ====================

    /**
     * 判断是否有效 IP（非空、非 unknown）
     */
    private static boolean isValidIp(String ip) {
        if (StrUtil.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            return false;
        }
        String normalizedIp = normalizeLoopback(ip.trim());
        return isIpv4(normalizedIp) || isIpv6(normalizedIp);
    }

    private static boolean isTrustedProxy(String remoteAddr) {
        List<String> trustedProxies = TrustedProxyProperties.getTrustedProxies();
        if (trustedProxies.isEmpty() || !isValidIp(remoteAddr)) {
            return false;
        }
        return trustedProxies.stream().anyMatch(cidr -> cidrMatch(cidr, remoteAddr));
    }

    private static boolean cidrMatch(String cidr, String remoteAddr) {
        if (StrUtil.isBlank(cidr) || !isValidIp(remoteAddr)) {
            return false;
        }

        String normalizedRemote = normalizeLoopback(remoteAddr.trim());
        String normalizedCidr = normalizeLoopback(cidr.trim());
        if (!normalizedCidr.contains("/")) {
            try {
                return InetAddress.getByName(normalizedCidr).equals(InetAddress.getByName(normalizedRemote));
            } catch (UnknownHostException e) {
                return false;
            }
        }

        String[] parts = normalizedCidr.split("/", 2);
        if (parts.length != 2) {
            return false;
        }

        try {
            InetAddress network = InetAddress.getByName(parts[0]);
            InetAddress target = InetAddress.getByName(normalizedRemote);
            byte[] networkBytes = network.getAddress();
            byte[] targetBytes = target.getAddress();
            if (networkBytes.length != targetBytes.length) {
                return false;
            }

            int prefixLength = Integer.parseInt(parts[1]);
            if (prefixLength < 0 || prefixLength > networkBytes.length * 8) {
                return false;
            }

            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (networkBytes[i] != targetBytes[i]) {
                    return false;
                }
            }

            if (remainingBits == 0) {
                return true;
            }

            int mask = (-1) << (8 - remainingBits);
            return (networkBytes[fullBytes] & mask) == (targetBytes[fullBytes] & mask);
        } catch (IllegalArgumentException | UnknownHostException e) {
            return false;
        }
    }

    private static String normalizeLoopback(String ip) {
        if (LOCALHOST_IPV6.equals(ip)) {
            return LOCALHOST_IP;
        }
        return ip;
    }

    /**
     * 判断是否为 IPv4
     */
    public static boolean isIpv4(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        return IPV4_PATTERN.matcher(ip).matches();
    }

    /**
     * 判断是否为 IPv6
     */
    public static boolean isIpv6(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        return ip.contains(":");
    }

    /**
     * 判断是否为内网 IP
     */
    public static boolean isInternalIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }

        // localhost
        if (LOCALHOST_IP.equals(ip) || LOCALHOST_IPV6.equals(ip) || "localhost".equalsIgnoreCase(ip)) {
            return true;
        }

        if (!isIpv4(ip)) {
            return false;
        }

        // 内网 IP 段判断
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);

            // 10.x.x.x
            if (first == 10) {
                return true;
            }
            // 172.16.x.x - 172.31.x.x
            if (first == 172 && second >= 16 && second <= 31) {
                return true;
            }
            // 192.168.x.x
            if (first == 192 && second == 168) {
                return true;
            }
        } catch (NumberFormatException ignored) {
        }

        return false;
    }

    /**
     * 判断是否为外网 IP
     */
    public static boolean isExternalIp(String ip) {
        return isIpv4(ip) && !isInternalIp(ip);
    }

    // ==================== IP 转换工具 ====================

    /**
     * IP 转 Long
     */
    public static long ipToLong(String ip) {
        if (!isIpv4(ip)) {
            return 0L;
        }
        String[] parts = ip.split("\\.");
        return (Long.parseLong(parts[0]) << 24)
                + (Long.parseLong(parts[1]) << 16)
                + (Long.parseLong(parts[2]) << 8)
                + Long.parseLong(parts[3]);
    }

    /**
     * Long 转 IP
     */
    public static String longToIp(long ipLong) {
        return ((ipLong >> 24) & 0xFF) + "."
                + ((ipLong >> 16) & 0xFF) + "."
                + ((ipLong >> 8) & 0xFF) + "."
                + (ipLong & 0xFF);
    }

    /**
     * 脱敏 IP（隐藏最后一段）
     */
    public static String maskIp(String ip) {
        if (!isIpv4(ip)) {
            return ip;
        }
        int lastDot = ip.lastIndexOf('.');
        return ip.substring(0, lastDot) + ".*";
    }

    /**
     * 获取 IP 地址（兼容旧方法名）
     */
    public static String getIpAddress(String ip) {
        return getCityInfo(ip);
    }
}
