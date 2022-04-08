package me.chulgil.spring.sample.trace;

import me.chulgil.spring.sample.trace.logtrace.LogTrace;

public abstract class AbstractTemplete<T> {

    private final LogTrace trace;

    protected AbstractTemplete(LogTrace trace) {
        this.trace = trace;
    }

    public T execute(String message) {
        TraceStatus status = null;
        try {
            status = trace.begin(message);

            // 로직 호출
            T result = call();

            trace.end(status);
            return result;
        } catch (Exception e) {
            trace.exception(status, e);
            throw e;
        }
    }

    protected abstract T call();

}
