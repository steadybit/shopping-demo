/*
 * Copyright 2025 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final RestClient restClient;

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

    @Value("${health.notification.host:#{null}}")
    private String notificationHost;
    @Value("${health.notification.port:8087}")
    private int notificationPort;

    public HealthController(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(
                        HttpClientSettings.defaults()
                                .withConnectTimeout(Duration.ofMillis(500))
                                .withReadTimeout(Duration.ofMillis(500))))
                .build();
    }

    @GetMapping("/dependencies")
    public Map<String, DependencyStatus> getDependencyHealth() {
        Map<String, DependencyStatus> results = new ConcurrentHashMap<>();
        results.put("gateway", new DependencyStatus("UP", "self", null));

        var futures = new java.util.ArrayList<CompletableFuture<Void>>();
        futures.add(CompletableFuture.runAsync(() -> results.put("fashion-bestseller", checkHttp(urlFashion))));
        futures.add(CompletableFuture.runAsync(() -> results.put("toys-bestseller", checkHttp(urlToys))));
        futures.add(CompletableFuture.runAsync(() -> results.put("hot-deals", checkHttp(urlHotDeals))));
        futures.add(CompletableFuture.runAsync(() -> results.put("checkout", checkHttp(urlCheckout))));
        futures.add(CompletableFuture.runAsync(() -> results.put("inventory", checkHttp(urlInventory))));
        futures.add(CompletableFuture.runAsync(() -> results.put("orders", checkTcp(ordersHost, ordersPort))));
        if (notificationHost != null) {
            futures.add(CompletableFuture.runAsync(() -> results.put("notification", checkTcp(notificationHost, notificationPort))));
        }
        if (activemqHost != null) {
            futures.add(CompletableFuture.runAsync(() -> results.put("activemq", checkTcp(activemqHost, activemqPort))));
        }
        if (redisHost != null) {
            futures.add(CompletableFuture.runAsync(() -> results.put("redis", checkTcp(redisHost, redisPort))));
        }
        if (kafkaHost != null) {
            futures.add(CompletableFuture.runAsync(() -> results.put("kafka", checkTcp(kafkaHost, kafkaPort))));
        }
        if (rabbitmqHost != null) {
            futures.add(CompletableFuture.runAsync(() -> results.put("rabbitmq", checkTcp(rabbitmqHost, rabbitmqPort))));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Preserve consistent ordering
        Map<String, DependencyStatus> ordered = new LinkedHashMap<>();
        for (String key : new String[]{"gateway", "fashion-bestseller", "toys-bestseller", "hot-deals",
                "checkout", "inventory", "orders", "notification", "activemq", "redis", "kafka", "rabbitmq"}) {
            if (results.containsKey(key)) {
                ordered.put(key, results.get(key));
            }
        }
        return ordered;
    }

    private DependencyStatus checkHttp(String url) {
        try {
            restClient.get().uri(url).retrieve().body(String.class);
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
            socket.connect(new InetSocketAddress(host, port), 500);
            return new DependencyStatus("UP", address, null);
        } catch (IOException e) {
            return new DependencyStatus("DOWN", address, e.getMessage());
        }
    }

    public record DependencyStatus(String status, String url, String error) {}
}
