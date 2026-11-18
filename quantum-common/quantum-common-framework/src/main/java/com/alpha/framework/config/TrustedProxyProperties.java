package com.alpha.framework.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Component
@ConfigurationProperties(prefix = "security.trusted-proxies")
public class TrustedProxyProperties {

    private static volatile List<Subnet> trustedProxies = List.of();

    private List<String> cidrs = List.of();

    public void setCidrs(List<String> cidrs) {
        this.cidrs = cidrs == null ? List.of() : List.copyOf(cidrs);
        var parsed = new ArrayList<Subnet>();
        for (String value : this.cidrs) {
            try {
                parsed.add(Subnet.parse(value));
            } catch (IllegalArgumentException e) {
                log.warn("忽略无效的可信代理网段");
            }
        }
        trustedProxies = List.copyOf(parsed);
    }

    public static boolean isTrusted(String address) {
        var candidates = trustedProxies;
        if (candidates.isEmpty() || address == null) {
            return false;
        }
        try {
            byte[] target = InetAddress.ofLiteral(address).getAddress();
            return candidates.stream().anyMatch(subnet -> subnet.contains(target));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private record Subnet(byte[] address, int fullBytes, int mask) {
        static Subnet parse(String value) {
            var parts = value.strip().split("/", 2);
            var address = InetAddress.ofLiteral(parts[0]).getAddress();
            int prefix = parts.length == 1 ? address.length * 8 : Integer.parseInt(parts[1]);
            if (prefix < 0 || prefix > address.length * 8) {
                throw new IllegalArgumentException("Invalid prefix length");
            }
            int remainingBits = prefix % 8;
            return new Subnet(address, prefix / 8, remainingBits == 0 ? 0 : 0xff << (8 - remainingBits));
        }

        boolean contains(byte[] target) {
            if (target.length != address.length) {
                return false;
            }
            for (int i = 0; i < fullBytes; i++) {
                if (address[i] != target[i]) {
                    return false;
                }
            }
            return mask == 0 || ((address[fullBytes] ^ target[fullBytes]) & mask) == 0;
        }
    }
}
