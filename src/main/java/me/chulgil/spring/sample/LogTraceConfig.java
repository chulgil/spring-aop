package me.chulgil.spring.sample;

import me.chulgil.spring.sample.trace.logtrace.FieldLogTrace;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogTraceConfig {

    @Bean
    public LogTrace logTrace() {
        return new FieldLogTrace();
    }
}
