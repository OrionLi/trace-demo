package io.github.orionli.tracebegin.controller;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author OrionLi
 */
@Slf4j
@RestController
public class TestController {

    @Autowired
    @Qualifier("MyExecutor")
    private Executor executor;

    @PostMapping("doTest")
    public String doTest(@RequestParam("name") String name) {
        log.info("入参 name={}", name);
        testTrace();
        executor.execute(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }

            log.info("异步线程执行完毕");
        });
        log.info("调用结束 name={}", name);
        return "Hello," + name;
    }

    private void testTrace() {
        log.info("这是来自testTrace的info日志");
        log.error("这是来自testTrace的error日志");
        testTrace2();
    }

    private void testTrace2() {
        log.info("这是来自testTrace2的info日志");
    }

}

