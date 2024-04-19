package io.github.orionli.tracebegin.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author OrionLi
 */
@Slf4j
@RestController
public class TestController {

    @PostMapping("doTest")
    public String doTest(@RequestParam("name") String name) {
        log.info("入参 name={}", name);
        testTrace();
        log.info("调用结束 name={}", name);
        return "Hello," + name;
    }

    private void testTrace() {
        log.info("这是一行info日志");
        log.error("这是一行error日志");
        testTrace2();
    }

    private void testTrace2() {
        log.info("这也是一行info日志");

    }

}

