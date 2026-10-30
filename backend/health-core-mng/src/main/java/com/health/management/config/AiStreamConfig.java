package com.health.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * AI 流式问答的线程池与心跳调度器。
 *
 * 不占用 Tomcat 请求线程：Controller 返回 SseEmitter 后，模型调用、写库都在
 * aiStreamExecutor 中执行；心跳线程只负责在等待首字期间发送 SSE 注释并探测断连。
 */
@Configuration
public class AiStreamConfig {

    @Bean(name = "aiStreamExecutor")
    public ThreadPoolTaskExecutor aiStreamExecutor(AiProperties properties) {
        int core = positive(properties.getStreamCorePoolSize(), 4);
        int max = Math.max(core, positive(properties.getStreamMaxPoolSize(), 8));
        int queue = properties.getStreamQueueCapacity() == null
                ? 50 : Math.max(0, properties.getStreamQueueCapacity());

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setThreadNamePrefix("ai-stream-");
        executor.setKeepAliveSeconds(60);
        // 队列满时直接拒绝，由 Controller 返回 SSE error，避免阻塞 Tomcat 线程
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return executor;
    }

    @Bean(name = "aiStreamHeartbeatScheduler", destroyMethod = "shutdownNow")
    public ScheduledExecutorService aiStreamHeartbeatScheduler() {
        return Executors.newScheduledThreadPool(1, runnable -> {
            Thread thread = new Thread(runnable, "ai-stream-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
    }

    private int positive(Integer value, int defaultValue) {
        if (value == null || value < 1) {
            return defaultValue;
        }
        return value;
    }
}
