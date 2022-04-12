package me.chulgil.spring.sample;

import me.chulgil.spring.util.CustomBeanNameGen;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class SampleApplication {

    public static void main(String[] args) {
        CustomBeanNameGen generator = new CustomBeanNameGen();
        generator.addBasePackages("me.chulgil.spring.sample");
        new SpringApplicationBuilder(SampleApplication.class)
                .beanNameGenerator(generator)
                .run(args);
    }
}
