package me.chulgil.spring.sample.trace.hellotrace;

import me.chulgil.spring.sample.trace.TraceStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HelloTraceV2Test {

    /**
     * 실행 로그
     * [41bbb3b7] hello
     * [41bbb3b7] hello time=5ms
     */
    @Test
    void begin_end() {
        HelloTraceV2 trace = new HelloTraceV2();
        TraceStatus status = trace.begin("hello");
        TraceStatus status2 = trace.beginSync(status.getTraceId(), "hello2");
        trace.end(status2);
        trace.end(status);
    }

    /**
     * 실행 로그
     * [898a3def] hello
     * [898a3def] hello time=13ms ex=java.lang.IllegalStateException
     */
    @Test
    void begin_exception() {
        HelloTraceV2 trace = new HelloTraceV2();
        TraceStatus status = trace.begin("hello");
        TraceStatus status2 = trace.beginSync(status.getTraceId(), "hello2");
        trace.exception(status2, new IllegalStateException());
        trace.exception(status, new IllegalStateException());
    }
}
