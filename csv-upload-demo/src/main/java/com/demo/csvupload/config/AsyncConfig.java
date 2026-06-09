package com.demo.csvupload.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configures the dedicated thread pool used for async CSV processing.
 *
 * <p>Keeping CSV processing on a separate executor prevents it from
 * competing with request-handling threads.
 */
@Configuration
public class AsyncConfig {

    @Value("${csv.processing.thread-pool-size:4}")
    private int poolSize;

    @Bean(name = "csvProcessingExecutor")
    public Executor csvProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("csv-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}

