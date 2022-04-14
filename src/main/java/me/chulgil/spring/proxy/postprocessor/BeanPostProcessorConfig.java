package me.chulgil.spring.proxy.postprocessor;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Configuration
@Import({AppConfigV1.class, AppConfigV2.class})
public class BeanPostProcessorConfig {

    @Bean
    public PackageLogTraceProxyPostProcessor logTraceProxyPostProcessor(LogTrace logTrace) {
        return new PackageLogTraceProxyPostProcessor("me.chulgil.spring.proxy.app", getAdvisor(logTrace));
    }

    private Advisor getAdvisor(LogTrace logTrace) {
        //pointcut
        NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
        pointcut.setMappedNames("request*", "order*", "save*");
        //advice
        LogTraceAdvice advice = new LogTraceAdvice(logTrace);
        //advisor = pointcut + advice
        return new DefaultPointcutAdvisor(pointcut, advice);
    }

}