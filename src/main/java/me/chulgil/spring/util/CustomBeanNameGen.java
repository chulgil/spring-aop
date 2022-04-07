package me.chulgil.spring.util;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

import java.util.ArrayList;
import java.util.List;

public class CustomBeanNameGen implements BeanNameGenerator {

    private final BeanNameGenerator generator = new AnnotationBeanNameGenerator();

    private List<String> basePackages = new ArrayList<>();

    @Override
    public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
        return isTargetPackageBean(definition) ? getBeanName(definition) : generator.generateBeanName(definition, registry);
    }

    private boolean isTargetPackageBean(BeanDefinition definition) {
        String beanClassName = getBeanName(definition);
        return basePackages.stream().anyMatch(beanClassName::startsWith);
    }

    private String getBeanName(BeanDefinition definition) {
        return definition.getBeanClassName();
    }

    public boolean addBasePackages(String path) {
        return this.basePackages.add(path);
    }
}


