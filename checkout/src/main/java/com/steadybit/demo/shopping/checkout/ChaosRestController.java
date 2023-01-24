/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.checkout;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PreDestroy;
import java.io.InterruptedIOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/checkout/chaos")
public class ChaosRestController {
    private static final Logger log = LoggerFactory.getLogger(ChaosRestController.class);
    private final JmsTemplate jmsTemplate;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(6);

    public ChaosRestController(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
        this.jmsTemplate.setReceiveTimeout(1_000L);
    }

    @PreDestroy
    public void shutdown() {
        this.scheduler.shutdown();
    }

    @PostMapping("/flood")
    public void flood(@RequestParam(defaultValue = "junk") String queueName) {
        var destination = new ActiveMQQueue(queueName);
        scheduler.execute(() -> {
            var future = scheduler.submit(() -> {
                log.info("Flooding queue {}", destination);
                var count = 0;
                try {
                    while (true) {
                        this.jmsTemplate.send(destination, session -> session.createTextMessage(RandomStringUtils.randomAlphanumeric(1_048_567)));
                        count++;
                        if (count % 100 == 0) {
                            log.info("Flooding {}. {} messages with 1mb sent.", destination, count);
                        }
                    }
                } catch (UncategorizedJmsException e) {
                    if (e.getCause() != null && e.getCause().getCause() instanceof InterruptedIOException) {
                        log.info("Stopped flooding {} after {} messages.", destination, count);
                    } else {
                        log.error("Stopped flooding {} after {} messages:", destination, count, e);
                    }
                }
                return null;
            });
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                future.cancel(true);
            }
        });
    }
}
