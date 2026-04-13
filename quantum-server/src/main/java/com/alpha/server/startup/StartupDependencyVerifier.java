package com.alpha.server.startup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 启动阶段外部依赖自检
 * <p>
 * 在应用打印“启动成功”信息之前主动验证数据库和 Redis 连通性。
 * 任一依赖不可用都直接抛错，阻止服务带病启动。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupDependencyVerifier implements SmartInitializingSingleton {

    private static final String DATABASE_PROBE_SQL = "SELECT 1";
    private static final String REDIS_PROBE_KEY = "__startup_dependency_check__";

    private final DataSource dataSource;
    private final RedissonClient redissonClient;
    private final Environment environment;

    @Override
    public void afterSingletonsInstantiated() {
        verifyDatabase();
        verifyRedis();
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
                    "Startup dependency check failed: database [" + datasourceUrl + "] - " + e.getMessage(),
                    e
            );
        }
    }

    private void verifyRedis() {
        String redisHost = environment.getProperty("spring.data.redis.host", "unknown");
        String redisPort = environment.getProperty("spring.data.redis.port", "6379");
        String redisDatabase = environment.getProperty("spring.data.redis.database", "0");
        String redisTarget = redisHost + ":" + redisPort + "/" + redisDatabase;

        log.info("Redis 依赖自检 | redis | {}", redisTarget);

        try {
            RBucket<Object> bucket = redissonClient.getBucket(REDIS_PROBE_KEY);
            bucket.isExists();
            log.info("Redis 依赖自检通过 | redis | {}", redisTarget);
        } catch (Exception e) {
            log.error("💔启动失败，请检查 Redis 连接 | redis | {}", redisTarget);
            throw new IllegalStateException(
                    "Startup dependency check failed: redis [" + redisTarget + "] - " + e.getMessage(),
                    e
            );
        }
    }
}
