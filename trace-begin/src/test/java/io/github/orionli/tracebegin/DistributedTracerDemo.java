package io.github.orionli.tracebegin;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.threadpool.TtlExecutors;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 详见<a href="https://github.com/alibaba/transmittable-thread-local">transmittable-thread-local</a>, 懒得看了
 * <a href="https://juejin.cn/post/7205777393453891644#heading-1">实现案例</a>
 */
public class DistributedTracerDemo {

    private static final Logger log = LoggerFactory.getLogger(DistributedTracerDemo.class);

    /**
     * 程序主入口
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        initiateRpcCall();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            log.error(e.getCause().toString());
        }
    }

    /**
     * 启动一次RPC调用过程
     */
    private static void initiateRpcCall() {
        // 从RPC上下文中获取跟踪ID和基础Span ID
        String traceId = "traceId_XXXYYY";
        String baseSpanId = "1.1";

        // 设置跨线程传递的跟踪信息
        distributedTracingInfo.set(new DtTransferInfo(traceId, baseSpanId));

        // 初始化叶Span ID信息
        traceIdToLeafSpanIdInfo.put(traceId, new LeafSpanIdInfo());

        // 增加Span ID引用计数
        incrementSpanIdRefCount();

        // 执行业务逻辑
        executeBusinessLogic();

        // 分布式追踪框架代码：减少Span ID引用计数
        decrementSpanIdRefCount();
    }

    /**
     * 使用带有跨线程上下文传递能力的线程池
     */
    private static final ExecutorService executorService = TtlExecutors.getTtlExecutorService(
            Executors.newFixedThreadPool(1, r -> {
                Thread t = new Thread(r, "Executors");
                t.setDaemon(true);
                return t;
            })
    );

    /**
     * 执行同步方法
     */
    private static void executeBusinessLogic() {
        // 使用TTL执行器异步调用 server 2，测试OK
        executorService.submit(() -> invokeServerWithRpc("server 2"));

        // 使用新线程异步调用 server 3（存在引用计数问题，待修复）
        new Thread(() -> invokeServerWithRpc("server 3"), "Thread-by-new").start();

        // 直接调用服务器（server 1）
        invokeServerWithRpc("server 1");
    }

    /**
     * 执行RPC调用
     *
     * @param server 服务器名称
     */
    private static void invokeServerWithRpc(String server) {
        // 分布式追踪框架代码

        // 获取并增加当前叶Span ID
        int leafSpanCurrent = incrementLeafSpanCurrentAndReturn();

        // 创建模拟的RPC上下文
        Map<String, String> rpcContext = new ConcurrentHashMap<>();

        // 设置跟踪ID和Span ID到上下文中
        rpcContext.put("traceId", getDistributedTracingInfo().traceId);
        rpcContext.put("spanId", getDistributedTracingInfo().baseSpanId + "." + leafSpanCurrent);

        // 执行RPC调用（此处省略）

        System.out.printf("Do Rpc invocation to server %s with %s%n", server, rpcContext);
    }

    /**
     * 跨线程传递的分布式追踪信息类
     */
    record DtTransferInfo(String traceId, String baseSpanId) {

    }

    /**
     * 叶Span ID信息类
     */
    static class LeafSpanIdInfo {

        final AtomicInteger current = new AtomicInteger(1);

        final AtomicInteger refCounter = new AtomicInteger(0);

    }

    /**
     * 存储跨线程传递的分布式追踪信息的ThreadLocal
     */
    private static final TransmittableThreadLocal<DtTransferInfo> distributedTracingInfo =
            new TransmittableThreadLocal<>() {
                /*
                @Override
                protected DtTransferInfo childValue(DtTransferInfo parentValue) {
                    // **注意**：
                    // 新建线程时，从父线程继承值时，计数加1
                    // 对应线程结束时，没有回调以清理ThreadLocal中的Context！，Bug！！
                    // InheritableThreadLocal 没有提供 对应的拦截方法。。。 计数不配对了。。。
                    // 但是一个线程就一个Context没清，线程数有限，Context占用内存一般很小，可以接受。

                    incrementSpanIdRefCount();

                    return super.childValue(parentValue);
                }
                 */

                @Override
                public void beforeExecute() {
                    super.beforeExecute();
                    DistributedTracerDemo.incrementSpanIdRefCount();
                }

                @Override
                public void afterExecute() {
                    DistributedTracerDemo.decrementSpanIdRefCount();
                }
            };

    /**
     * 存储叶Span ID信息的映射表
     */
    private static final ConcurrentHashMap<String, LeafSpanIdInfo> traceIdToLeafSpanIdInfo = new ConcurrentHashMap<>();

    /**
     * 获取跨线程传递的分布式追踪信息
     *
     * @return 分布式追踪信息实例
     */
    private static DtTransferInfo getDistributedTracingInfo() {
        return distributedTracingInfo.get();
    }

    /**
     * 增加Span ID引用计数
     */
    private static void incrementSpanIdRefCount() {
        String traceId = getDistributedTracingInfo().traceId;
        int refCounter = traceIdToLeafSpanIdInfo.get(traceId).refCounter.incrementAndGet();

        System.out.printf("DEBUG: Increase reference counter(%s) for traceId %s in thread %s%n", refCounter, traceId,
                Thread.currentThread().getName());
    }

    /**
     * 减少Span ID引用计数
     */
    private static void decrementSpanIdRefCount() {
        String traceId = getDistributedTracingInfo().traceId;
        LeafSpanIdInfo leafSpanIdInfo = traceIdToLeafSpanIdInfo.get(traceId);

        int refCounter = leafSpanIdInfo.refCounter.decrementAndGet();
        System.out.printf("DEBUG: Decrease reference counter(%s) for traceId %s in thread %s%n", refCounter, traceId,
                Thread.currentThread().getName());

        if (refCounter == 0) {
            traceIdToLeafSpanIdInfo.remove(traceId);

            System.out.printf("DEBUG: Clear traceIdToLeafSpanIdInfo for traceId %s in thread %s%n", traceId,
                    Thread.currentThread().getName());
        } else if (refCounter < 0) {
            throw new IllegalStateException("Leaf Span Id Info Reference counter has Bug!!");
        }
    }

    /**
     * 增加当前叶Span ID并返回新的值
     *
     * @return 当前叶Span ID的新值
     */
    private static int incrementLeafSpanCurrentAndReturn() {
        String traceId = getDistributedTracingInfo().traceId;
        return traceIdToLeafSpanIdInfo.get(traceId).current.getAndIncrement();
    }

}