package io.github.orionli.tracebegin.config;

import java.io.Serial;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import jakarta.annotation.Nonnull;

/**
 * @author OrionLi
 */
public final class MyThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

    @Serial
    private static final long serialVersionUID = 1638875899703527776L;

    public MyThreadPoolTaskExecutor() {
        super();
    }

    @Override
    public void execute(@Nonnull Runnable task) {
        super.execute(ThreadMdcUtil.wrap(task, MDC.getCopyOfContextMap()));
    }

    @Override
    public <T> Future<T> submit(@Nonnull Callable<T> task) {
        return super.submit(ThreadMdcUtil.wrap(task, MDC.getCopyOfContextMap()));
    }

    @Override
    public Future<?> submit(@Nonnull Runnable task) {
        return super.submit(ThreadMdcUtil.wrap(task, MDC.getCopyOfContextMap()));
    }

}
