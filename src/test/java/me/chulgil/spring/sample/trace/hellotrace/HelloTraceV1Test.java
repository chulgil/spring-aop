package me.chulgil.spring.sample.trace.hellotrace;

import me.chulgil.spring.sample.trace.TraceStatus;
import org.junit.jupiter.api.Test;

public class HelloTraceV1Test {

    /**
     * 실행 로그
     * [41bbb3b7] hello
     * [41bbb3b7] hello time=5ms
     */
    @Test
    void begin_end() {
        HelloTraceV1 trace = new HelloTraceV1();
        TraceStatus status = trace.begin("hello");
        trace.end(status);
    }

    /**
     * 실행 로그
     * [898a3def] hello
     * [898a3def] hello time=13ms ex=java.lang.IllegalStateException
     */
    @Test
    void begin_exception() {
        HelloTraceV1 trace = new HelloTraceV1();
        TraceStatus status = trace.begin("hello");
        trace.exception(status, new IllegalStateException());
    }
}
