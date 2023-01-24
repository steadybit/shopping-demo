/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.checkout;

import com.steadybit.shopping.domain.Order;
import com.steadybit.shopping.domain.OrderItem;
import com.steadybit.shopping.domain.ShoppingCart;
import org.apache.activemq.command.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashSet;

@RestController
@RequestMapping("/checkout")
public class CheckoutRestController {
    private static final Logger log = LoggerFactory.getLogger(CheckoutRestController.class);
    private final ActiveMQQueue destination = new ActiveMQQueue("order_created");
    private final JmsTemplate jms;
    private final CartRepository repository;

    public CheckoutRestController(JmsTemplate jmsTemplate, CartRepository repository) {
        this.jms = jmsTemplate;
        this.repository = repository;
    }

    @PostMapping("/direct")
    @Transactional(timeout = 5)
    public void checkoutDirect(@RequestBody ShoppingCart cart) {
        this.jms.convertAndSend(destination, toOrder(toCart(cart)));
        log.info("Published direct order {}", cart.getId());
    }

    @PostMapping("/buffered")
    @Transactional(timeout = 5)
    public void checkoutAsync(@RequestBody ShoppingCart cart) {
        this.repository.save(toCart(cart));
        log.info("Buffered order: {}", cart.getId());
    }

    @Scheduled(fixedDelay = 1_000L)
    @Transactional(timeout = 5)
    public void publishPendingOrders() {
        while (true) {
            var publishPending = this.repository.findPublishPending(PageRequest.ofSize(5));

            var published = new HashSet<String>();
            for (Cart cart : publishPending) {
                this.jms.convertAndSend(destination, toOrder(cart));
                published.add(cart.getId());
                log.info("Published buffered order {}", cart.getId());
            }

            this.repository.markAsPublished(published, Instant.now());
            if (!publishPending.hasNext()) {
                return;
            }
        }
    }

    private Cart toCart(ShoppingCart cart) {
        Cart c = new Cart();
        c.setSubmitted(Instant.now());
        c.setId(cart.getId());
        c.setItems(cart.getItems().stream().map(i -> new Cart.Item(i.getProductId(), i.getQuantity(), i.getPrice())).toList());
        return c;
    }

    private Order toOrder(Cart cart) {
        Order o = new Order();
        o.setId(cart.getId());
        o.setSubmitted(cart.getSubmitted());
        o.setItems(cart.getItems().stream().map(i -> new OrderItem(i.getId(), i.getQuantity(), i.getPrice())).toList());
        return o;
    }
}
