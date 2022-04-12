package me.chulgil.spring.proxy;

import me.chulgil.spring.proxy.app.v4.InterfaceProxyConfig;
import me.chulgil.spring.proxy.app.v5.ConcreteProxyConfig;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;
import me.chulgil.spring.sample.trace.logtrace.ThreadLocalLogTrace;
import me.chulgil.spring.util.CustomBeanNameGen;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import(ConcreteProxyConfig.class)
@SpringBootApplication(scanBasePackages="me.chulgil.spring.proxy.app.v5")
public class ProxyApplication {

    public static void main(String[] args) {
        CustomBeanNameGen generator = new CustomBeanNameGen();
        generator.addBasePackages("me.chulgil.spring.proxy.app");
        new SpringApplicationBuilder(ProxyApplication.class)
                .beanNameGenerator(generator)
                .run(args);
    }

    @Bean
    public LogTrace logTrace() {
        return new ThreadLocalLogTrace();
    }
}
