package me.chulgil.spring.sample;

import me.chulgil.spring.sample.trace.logtrace.LogTrace;
import me.chulgil.spring.sample.trace.logtrace.ThreadLocalLogTrace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogTraceConfig {

    @Bean
    public LogTrace logTrace() {
        return new ThreadLocalLogTrace();
    }
}
