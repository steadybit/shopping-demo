/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.checkout;

import com.steadybit.shopping.domain.Order;
import com.steadybit.testcontainers.Steadybit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CheckoutApplicationTest {
    @Container
    public static final ActiveMqContainer broker = new ActiveMqContainer("symptoma/activemq:latest");
    private final String CART_ID = UUID.randomUUID().toString();
    private final HttpEntity<String> CHECKOUT_CART_BODY = asJsonBody("""
            {
                "id" : "{{CART_ID}}",
                "items" : [
                    { "productId" : "368ae4a7-acab-419d-93f7-127df91fb695", "quantity" : 1, "price" : 299.99 }
                ]
            }
            """.replace("{{CART_ID}}", CART_ID));
    @Autowired
    TestRestTemplate http;
    @Autowired
    JmsTemplate jms;

    @BeforeEach
    void setUp() {
        this.drainJmsQueue();
    }

    @Test
    void should_checkout_cart_and_create_order() {
        http.postForObject("/checkout/direct", CHECKOUT_CART_BODY, Void.class);
        var order = (Order) jms.receiveAndConvert("order_created");
        assertThat(order.getId()).isEqualTo(CART_ID);
    }

    @Test
    void should_checkout_cart_within_1s_and_create_order_afterwards_given_a_slow_brocker() {
        Steadybit.networkDelayPackages(Duration.ofSeconds(2L))
                .forContainers(broker)
                .exec(() -> assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
                    http.postForObject("/checkout/buffered", CHECKOUT_CART_BODY, Void.class);
                }));

        await().untilAsserted(() -> {
            var order = (Order) jms.receiveAndConvert("order_created");
            assertThat(order.getId()).isEqualTo(CART_ID);
        });
    }

    @Test
    void should_checkout_cart_within_1s_and_create_order_afterwards_given_an_offline_brocker() {
        Steadybit.networkBlackhole()
                .forContainers(broker)
                .exec(() -> assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
                    http.postForObject("/checkout/buffered", CHECKOUT_CART_BODY, Void.class);
                }));

        await().untilAsserted(() -> {
            var order = (Order) jms.receiveAndConvert("order_created");
            assertThat(order.getId()).isEqualTo(CART_ID);
        });
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.activemq.broker-url", broker::getBrokerUrl);
    }

    @TestConfiguration
    static class TestConfig implements AsyncConfigurer {

        @Bean
        public RestTemplateBuilder restTemplateBuilder() {
            return new RestTemplateBuilder().setReadTimeout(Duration.ofSeconds(2));
        }
    }

    private static HttpEntity<String> asJsonBody(String s) {
        return new HttpEntity<>(s, new LinkedMultiValueMap<>(Map.of(HttpHeaders.CONTENT_TYPE, List.of(MediaType.APPLICATION_JSON_VALUE))));
    }

    private void drainJmsQueue() {
        long oldTimeout = jms.getReceiveTimeout();
        jms.setReceiveTimeout(10);
        while (jms.receiveAndConvert("order_created") != null) {
            //NOP
        }
        jms.setReceiveTimeout(oldTimeout);
    }
}