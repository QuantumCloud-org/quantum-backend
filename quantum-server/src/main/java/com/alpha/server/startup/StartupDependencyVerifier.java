package com.alpha.server.startup;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 启动阶段外部依赖自检
 * <p>
 * - 数据库：始终验证连通性
 * - 缓存：根据 {@code cache.mode} 决定验证 Redis 还是打印 Caffeine 就绪日志
 */
@Slf4j
@Component
public class StartupDependencyVerifier implements SmartInitializingSingleton {

    private static final String DATABASE_PROBE_SQL = "SELECT 1";
    private static final String REDIS_PROBE_KEY = "__startup_dependency_check__";

    private final DataSource dataSource;
    private final Environment environment;
    @Nullable
    private final RedissonClient redissonClient;

    /**
     * {@code RedissonClient} 在 local 模式下不存在，通过 {@code required=false} 避免注入失败。
     */
    public StartupDependencyVerifier(DataSource dataSource,
                                     Environment environment,
                                     @Autowired(required = false) RedissonClient redissonClient) {
        this.dataSource = dataSource;
        this.environment = environment;
        this.redissonClient = redissonClient;
    }

    @Override
    public void afterSingletonsInstantiated() {
        verifyDatabase();
        verifyCache();
    }

    private void verifyDatabase() {
        String datasourceUrl = environment.getProperty("spring.datasource.url", "unknown");
        log.info("数据库依赖自检 | database | {}", datasourceUrl);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(DATABASE_PROBE_SQL)) {
            if (!resultSet.next() || resultSet.getInt(1) != 1) {
                throw new IllegalStateException("database probe returned unexpected result");
            }
            log.info("数据库依赖自检通过 | database | {}", datasourceUrl);
        } catch (Exception e) {
            log.error("启动失败，请检查数据库连接 | database | {}", datasourceUrl);
            throw new IllegalStateException(
                    "Startup dependency check failed: database [" + datasourceUrl + "] - " + e.getMessage(), e);
        }
    }

    private void verifyCache() {
        String cacheMode = environment.getProperty("cache.mode", "redis");

        if ("local".equals(cacheMode)) {
            log.info("缓存模式: LOCAL (Caffeine-only) | 跳过 Redis 自检 | Caffeine 启动成功");
            return;
        }

        verifyRedis();
    }

    private void verifyRedis() {
        String redisHost = environment.getProperty("spring.data.redis.host", "unknown");
        String redisPort = environment.getProperty("spring.data.redis.port", "6379");
        String redisDatabase = environment.getProperty("spring.data.redis.database", "0");
        String redisTarget = redisHost + ":" + redisPort + "/" + redisDatabase;

        log.info("Redis 依赖自检 | redis | {}", redisTarget);

        if (redissonClient == null) {
            throw new IllegalStateException(
                    "Startup dependency check failed: cache.mode=redis but RedissonClient bean is missing. " +
                    "Check Redis configuration or switch to cache.mode=local.");
        }

        try {
            RBucket<Object> bucket = redissonClient.getBucket(REDIS_PROBE_KEY);
            bucket.isExists();
            log.info("Redis 依赖自检通过 | redis | {}", redisTarget);
        } catch (Exception e) {
            log.error("启动失败，请检查 Redis 连接 | redis | {}", redisTarget);
            throw new IllegalStateException(
                    "Startup dependency check failed: redis [" + redisTarget + "] - " + e.getMessage(), e);
        }
    }
}
