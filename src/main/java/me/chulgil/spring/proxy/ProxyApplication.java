package me.chulgil.spring.proxy;

import me.chulgil.spring.proxy.dynamic.DynamicProxyFilterConfig;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;
import me.chulgil.spring.sample.trace.logtrace.ThreadLocalLogTrace;
import me.chulgil.spring.util.CustomBeanNameGen;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import(DynamicProxyFilterConfig.class)
@SpringBootApplication(scanBasePackages = "me.chulgil.spring.proxy.dynamic")
public class ProxyApplication {

    public static void main(String[] args) {
        CustomBeanNameGen generator = new CustomBeanNameGen();
        generator.addBasePackages("me.chulgil.spring.proxy.dynamic");
        new SpringApplicationBuilder(ProxyApplication.class)
                .beanNameGenerator(generator)
                .run(args);
    }

    @Bean
    public LogTrace logTrace() {
        return new ThreadLocalLogTrace();
    }
}
