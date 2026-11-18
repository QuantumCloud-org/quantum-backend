package com.alpha.cache.config;

import com.alpha.cache.util.RedisUtil;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.ConstantDelay;
import org.redisson.config.SingleServerConfig;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.redisson.spring.starter.RedissonAutoConfigurationV4;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.regex.Pattern;

@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@Import(RedissonAutoConfigurationV4.class)
@ConditionalOnProperty(name = "cache.mode", havingValue = "redis", matchIfMissing = true)
public class RedisConfig {

    private final RedissonPoolProperties poolProperties;

    /**
     * Redisson 配置定制
     * <p>
     * Redisson starter 只从 spring.data.redis.* 读取基础连接信息，
     * 连接池/超时/重试等高级参数通过此 customizer 从 RedissonPoolProperties 注入。
     */
    @Bean
    public RedissonAutoConfigurationCustomizer redissonCustomizer() {
        return config -> {
            config.setCodec(new CacheJsonCodec());

            SingleServerConfig ssc = config.useSingleServer();
            ssc.setConnectionMinimumIdleSize(poolProperties.getConnectionMinimumIdleSize());
            ssc.setConnectionPoolSize(poolProperties.getConnectionPoolSize());
            ssc.setConnectTimeout(poolProperties.getConnectTimeout());
            ssc.setTimeout(poolProperties.getTimeout());
            ssc.setRetryAttempts(poolProperties.getRetryAttempts());
            ssc.setRetryDelay(new ConstantDelay(Duration.ofMillis(poolProperties.getRetryInterval())));
            ssc.setSubscriptionConnectionMinimumIdleSize(poolProperties.getSubscriptionConnectionMinimumIdleSize());
            ssc.setSubscriptionConnectionPoolSize(poolProperties.getSubscriptionConnectionPoolSize());
            ssc.setPingConnectionInterval(poolProperties.getPingConnectionInterval());

            log.info("Redisson 配置完成 | codec=JsonJacksonCodec | pool={}/{} | timeout={}ms | retry={}x{}ms",
                    poolProperties.getConnectionMinimumIdleSize(),
                    poolProperties.getConnectionPoolSize(),
                    poolProperties.getTimeout(),
                    poolProperties.getRetryAttempts(),
                    poolProperties.getRetryInterval());
        };
    }

    @Bean
    public RedisUtil redisUtil(RedissonClient redissonClient) {
        return new RedisUtil(redissonClient);
    }
    /** Keeps Redisson's existing wire format while restricting the type IDs it accepts. */
    public static final class CacheJsonCodec extends JsonJacksonCodec {
        private static final Pattern PRIMITIVE_ARRAY = Pattern.compile("\\[+[ZBCSIJFD]");
        private static final Pattern OBJECT_ARRAY = Pattern.compile("\\[+L([^;]+);");

        public CacheJsonCodec() {
            getObjectMapper().registerModule(new JavaTimeModule());
            getObjectMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }

        public CacheJsonCodec(ClassLoader loader, CacheJsonCodec codec) {
            super(loader, codec);
        }

        @Override
        protected PolymorphicTypeValidator createPolymorphicTypeValidator() {
            return new PolymorphicTypeValidator.Base() {
                @Override
                public Validity validateSubClassName(MapperConfig<?> config, JavaType base, String typeName) {
                    return isAllowedType(typeName) ? Validity.INDETERMINATE : Validity.DENIED;
                }

                @Override
                public Validity validateSubType(MapperConfig<?> config, JavaType base, JavaType subtype) {
                    return isAllowedType(subtype) ? Validity.ALLOWED : Validity.DENIED;
                }
            };
        }

        private static boolean isAllowedType(JavaType type) {
            return isAllowedType(type.getRawClass().getName())
                    && type.getBindings().getTypeParameters().stream().allMatch(CacheJsonCodec::isAllowedType);
        }

        private static boolean isAllowedType(String name) {
            if (isAllowedClass(name) || PRIMITIVE_ARRAY.matcher(name).matches()) {
                return true;
            }
            var array = OBJECT_ARRAY.matcher(name);
            return array.matches() && isAllowedClass(array.group(1));
        }

        private static boolean isAllowedClass(String name) {
            return name.startsWith("com.alpha.") || name.startsWith("java.util.")
                    || name.startsWith("java.time.") || name.startsWith("java.lang.");
        }
    }
}
