package com.alpha.logging.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步任务配置
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean("asyncExecutor")
    public Executor getAsyncExecutor() {
        var executor = auditExecutor(2, 4, 256, "audit-");
        executor.setRejectedExecutionHandler((task, pool) -> {
            try {
                if (pool.isShutdown() || !pool.getQueue().offer(task, 1, TimeUnit.SECONDS)) {
                    throw new RejectedExecutionException("Audit executor queue is full or shutting down");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Interrupted while queuing an audit event", e);
            }
        });
        return executor;
    }

    @Bean("loginAuditExecutor")
    public ThreadPoolTaskExecutor loginAuditExecutor() {
        return auditExecutor(1, 2, 128, "login-audit-");
    }

    private ThreadPoolTaskExecutor auditExecutor(int coreSize, int maxSize, int queueCapacity, String threadPrefix) {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadFactory(Thread.ofPlatform().inheritInheritableThreadLocals(false).name(threadPrefix, 0).factory());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncExceptionHandler();
    }

    /**
     * 异步异常处理器
     */
    public static class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error("异步任务异常 | method={}.{} | errorType={}",
                    method.getDeclaringClass().getName(), method.getName(), ex.getClass().getName());
        }
    }
}
