/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final RestTemplate restTemplate;

    @Value("${rest.endpoint.fashion}")
    private String urlFashion;
    @Value("${rest.endpoint.toys}")
    private String urlToys;
    @Value("${rest.endpoint.hotdeals}")
    private String urlHotDeals;
    @Value("${rest.endpoint.checkout}")
    private String urlCheckout;
    @Value("${rest.endpoint.inventory}")
    private String urlInventory;

    @Value("${health.activemq.host:#{null}}")
    private String activemqHost;
    @Value("${health.activemq.port:61613}")
    private int activemqPort;

    @Value("${health.redis.host:#{null}}")
    private String redisHost;
    @Value("${health.redis.port:6379}")
    private int redisPort;

    @Value("${health.kafka.host:#{null}}")
    private String kafkaHost;
    @Value("${health.kafka.port:9092}")
    private int kafkaPort;

    @Value("${health.rabbitmq.host:#{null}}")
    private String rabbitmqHost;
    @Value("${health.rabbitmq.port:5672}")
    private int rabbitmqPort;

    @Value("${health.orders.host:orders}")
    private String ordersHost;
    @Value("${health.orders.port:8086}")
    private int ordersPort;

    @Value("${health.notification.host:notification}")
    private String notificationHost;
    @Value("${health.notification.port:8087}")
    private int notificationPort;

    public HealthController(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(2))
                .build();
    }

    @GetMapping("/dependencies")
    public Map<String, DependencyStatus> getDependencyHealth() {
        Map<String, DependencyStatus> status = new LinkedHashMap<>();
        status.put("gateway", new DependencyStatus("UP", "self", null));
        status.put("fashion-bestseller", checkHttp(urlFashion));
        status.put("toys-bestseller", checkHttp(urlToys));
        status.put("hot-deals", checkHttp(urlHotDeals));
        status.put("checkout", checkHttp(urlCheckout));
        status.put("inventory", checkHttp(urlInventory));
        status.put("orders", checkTcp(ordersHost, ordersPort));
        status.put("notification", checkTcp(notificationHost, notificationPort));
        if (activemqHost != null) {
            status.put("activemq", checkTcp(activemqHost, activemqPort));
        }
        if (redisHost != null) {
            status.put("redis", checkTcp(redisHost, redisPort));
        }
        if (kafkaHost != null) {
            status.put("kafka", checkTcp(kafkaHost, kafkaPort));
        }
        if (rabbitmqHost != null) {
            status.put("rabbitmq", checkTcp(rabbitmqHost, rabbitmqPort));
        }
        return status;
    }

    private DependencyStatus checkHttp(String url) {
        try {
            restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            return new DependencyStatus("UP", url, null);
        } catch (RestClientResponseException e) {
            return new DependencyStatus("UP", url, null);
        } catch (Exception e) {
            return new DependencyStatus("DOWN", url, e.getMessage());
        }
    }

    private DependencyStatus checkTcp(String host, int port) {
        String address = host + ":" + port;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
            return new DependencyStatus("UP", address, null);
        } catch (IOException e) {
            return new DependencyStatus("DOWN", address, e.getMessage());
        }
    }

    public record DependencyStatus(String status, String url, String error) {}
}
