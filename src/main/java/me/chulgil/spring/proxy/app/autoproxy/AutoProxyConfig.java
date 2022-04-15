package me.chulgil.spring.proxy.app.autoproxy;

import me.chulgil.spring.proxy.app.v1.AppConfigV1;
import me.chulgil.spring.proxy.app.v2.AppConfigV2;
import me.chulgil.spring.proxy.factory.advice.LogTraceAdvice;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({AppConfigV1.class, AppConfigV2.class})
public class AutoProxyConfig {

    @Bean
    public Advisor advisor1(LogTrace logTrace) {
        NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
        pointcut.setMappedNames("request*", "order*", "save*");
        LogTraceAdvice advice = new LogTraceAdvice(logTrace);
        return new DefaultPointcutAdvisor(pointcut, advice);
    }

}
