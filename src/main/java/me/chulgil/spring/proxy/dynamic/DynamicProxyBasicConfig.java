package me.chulgil.spring.proxy.dynamic;

import me.chulgil.spring.proxy.app.v1.IOrderController;
import me.chulgil.spring.proxy.app.v1.IOrderRepository;
import me.chulgil.spring.proxy.app.v1.IOrderService;
import me.chulgil.spring.proxy.app.v1.OrderController;
import me.chulgil.spring.proxy.app.v1.OrderRepository;
import me.chulgil.spring.proxy.app.v1.OrderService;
import me.chulgil.spring.sample.trace.logtrace.LogTrace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Proxy;

@Configuration
public class DynamicProxyBasicConfig {

    @Bean
    public IOrderController orderController(LogTrace logTrace) {
        IOrderController controller = new OrderController(orderService(logTrace));
        IOrderController proxy = (IOrderController) Proxy.newProxyInstance(
                IOrderController.class.getClassLoader(),
                new Class[]{IOrderController.class},
                new LogTraceBasicHandler(controller, logTrace)
        );
        return proxy;
    }

    @Bean
    public IOrderService orderService(LogTrace logTrace) {
        IOrderService service = new OrderService(orderRepository(logTrace));
        IOrderService proxy = (IOrderService) Proxy.newProxyInstance(
                IOrderService.class.getClassLoader(),
                new Class[]{IOrderService.class},
                new LogTraceBasicHandler(service, logTrace)
        );
        return proxy;
    }

    @Bean
    public IOrderRepository orderRepository(LogTrace logTrace) {
        IOrderRepository repository = new OrderRepository();
        IOrderRepository proxy = (IOrderRepository) Proxy.newProxyInstance(
                IOrderRepository.class.getClassLoader(),
                new Class[]{IOrderRepository.class},
                new LogTraceBasicHandler(repository, logTrace)
        );
        return proxy;
    }
}
