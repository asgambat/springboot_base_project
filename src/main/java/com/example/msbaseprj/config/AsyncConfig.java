package com.example.msbaseprj.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Value("${app.pool.core-size:3}")
    private int corePoolSize;

    @Value("${app.pool.max-size:10}")
    private int maxPoolSize;

    @Value("${app.pool.queue-capacity:100}")
    private int queueCapacity;

    @Value("${app.pool.thread-name-prefix:async-fetch-}")
    private String threadNamePrefix;

    @Bean(name = "appTaskExecutor")
    public Executor appTaskExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setThreadFactory(new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                var thread = new Thread(r, threadNamePrefix + counter.getAndIncrement());
                thread.setDaemon(false);
                return thread;
            }
        });
        // Add MDC propagation via TaskDecorator
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    private static class MdcTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // Capture MDC context from the submitting (parent) thread
            var parentMdcContext = MDC.getCopyOfContextMap();

            return () -> {
                // Preserve any existing MDC context in the child thread (for reuse safety)
                var originalChildMdcContext = MDC.getCopyOfContextMap();
                try {
                    // Restore parent MDC context in the child thread
                    if (parentMdcContext != null) 
                        MDC.setContextMap(parentMdcContext);
                    else MDC.clear();
                    
                    // Execute the original task
                    runnable.run();
                } finally {
                    // Restore original child MDC context to prevent leaks
                    if (originalChildMdcContext != null) 
                        MDC.setContextMap(originalChildMdcContext);
                    else MDC.clear();
                }
            };
        }
        
    }

}
