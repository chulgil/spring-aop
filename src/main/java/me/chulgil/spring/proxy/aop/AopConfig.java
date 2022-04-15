package me.chulgil.spring.proxy.aop;

import me.chulgil.spring.proxy.app.v1.AppConfigV1;
import me.chulgil.spring.proxy.app.v2.AppConfigV2;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({AppConfigV1.class, AppConfigV2.class})
public class AopConfig {
    @Bean
    public LogTraceAspect logTraceAspect(LogTrace logTrace) {
        return new LogTraceAspect(logTrace);
    }
}
