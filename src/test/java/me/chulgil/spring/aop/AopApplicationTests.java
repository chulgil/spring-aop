package me.chulgil.spring.aop;

import lombok.extern.slf4j.Slf4j;
import me.chulgil.spring.aop.order.OrderRepository;
import me.chulgil.spring.aop.order.OrderService;
import me.chulgil.spring.aop.order.aop.AspectV4Pointcut;
import me.chulgil.spring.aop.order.aop.AspectV5Order;
import me.chulgil.spring.aop.order.aop.AspectV6Advice;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@Import({AspectV6Advice.class})
@SpringBootTest
class AopApplicationTests {

	@Autowired
	OrderService orderService;

	@Autowired
	OrderRepository orderRepository;

	@Test
	void aopInfo() {
		log.info("isAopProxy, orderService={}", AopUtils.isAopProxy(orderService));
		log.info("isAopProxy, orderRepository={}", AopUtils.isAopProxy(orderRepository));
	}

	@Test
	void success() {
		orderService.orderItem("itemA");
	}

	@Test
	void exception() {
		assertThatThrownBy(()-> orderService.orderItem("ex"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void contextLoads() {
	}

}
