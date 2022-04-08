package me.chulgil.spring.sample.trace.callback;

public interface TraceCallback<T> {
    T call();
}
