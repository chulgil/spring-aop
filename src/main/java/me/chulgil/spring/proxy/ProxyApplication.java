package me.chulgil.spring.proxy;

import me.chulgil.spring.util.CustomBeanNameGen;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class ProxyApplication {

    public static void main(String[] args) {
        CustomBeanNameGen generator = new CustomBeanNameGen();
        generator.addBasePackages("me.chulgil.spring.proxy.app");
        new SpringApplicationBuilder(ProxyApplication.class)
                .beanNameGenerator(generator)
                .run(args);
    }
}
