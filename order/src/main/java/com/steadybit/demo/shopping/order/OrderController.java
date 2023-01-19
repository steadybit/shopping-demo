package com.steadybit.demo.shopping.order;

import com.steadybit.shopping.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @JmsListener(destination = "order_created")
    public void orderCreated(Order order) {
        log.info("Order created: {}", order);
    }

}
