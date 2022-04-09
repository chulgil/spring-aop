package me.chulgil.spring.proxy;

import me.chulgil.spring.proxy.app.v1.AppConfig;
import me.chulgil.spring.util.CustomBeanNameGen;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;

@Import(AppConfig.class)
@SpringBootApplication(scanBasePackages = "me.chulgil.spring.proxy.app")
public class ProxyApplication {

    public static void main(String[] args) {
        CustomBeanNameGen generator = new CustomBeanNameGen();
        generator.addBasePackages("me.chulgil.spring");
        new SpringApplicationBuilder(ProxyApplication.class)
                .beanNameGenerator(generator)
                .run(args);
    }
}
