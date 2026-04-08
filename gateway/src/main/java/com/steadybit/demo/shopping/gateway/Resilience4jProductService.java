/*
 * Copyright 2023 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.gateway;

import com.steadybit.shopping.domain.Product;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
public class Resilience4jProductService {
    private static final Logger log = LoggerFactory.getLogger(Resilience4jProductService.class);

    @Value("${rest.endpoint.fashion}")
    private String urlFashion;
    @Value("${rest.endpoint.toys}")
    private String urlToys;
    @Value("${rest.endpoint.hotdeals}")
    private String urlHotDeals;

    private final RestClient restClient;
    private final ParameterizedTypeReference<List<Product>> productListTypeReference = new ParameterizedTypeReference<List<Product>>() {
    };

    public Resilience4jProductService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(
                        HttpClientSettings.defaults()
                                .withConnectTimeout(Duration.ofSeconds(2))
                                .withReadTimeout(Duration.ofSeconds(2))))
                .build();
    }

    @Retry(name = "fashion", fallbackMethod = "getProductsFallbackRetry")
    public List<Product> getFashionWithRetry() {
        return this.restClient.get().uri(this.urlFashion).retrieve().body(this.productListTypeReference);
    }

    @Retry(name = "toys", fallbackMethod = "getProductsFallbackRetry")
    public List<Product> getToysWithRetry() {
        return this.restClient.get().uri(this.urlToys).retrieve().body(this.productListTypeReference);
    }

    @Retry(name = "hotdeals", fallbackMethod = "getProductsFallbackRetry")
    public List<Product> getHotDealsWithRetry() {
        return this.restClient.get().uri(this.urlHotDeals).retrieve().body(this.productListTypeReference);
    }

    @Retry(name = "fashion", fallbackMethod = "getProductsFallbackRetry")
    @CircuitBreaker(name = "fashion", fallbackMethod = "getProductsFallbackCircuitBreaker")
    public List<Product> getFashionWithRetryAndCircuitBreaker() {
        return this.restClient.get().uri(this.urlFashion).retrieve().body(this.productListTypeReference);
    }

    @Retry(name = "toys", fallbackMethod = "getProductsFallbackRetry")
    @CircuitBreaker(name = "toys", fallbackMethod = "getProductsFallbackCircuitBreaker")
    public List<Product> getToysWithRetryAndCircuitBreaker() {
        return this.restClient.get().uri(this.urlToys).retrieve().body(this.productListTypeReference);
    }

    @Retry(name = "hotdeals", fallbackMethod = "getProductsFallbackRetry")
    @CircuitBreaker(name = "hotdeals", fallbackMethod = "getProductsFallbackCircuitBreaker")
    public List<Product> getHotDealsWithRetryAndCircuitBreaker() {
        return this.restClient.get().uri(this.urlHotDeals).retrieve().body(this.productListTypeReference);
    }

    private List<Product> getProductsFallbackRetry(RuntimeException exception) {
        log.info("resilience4j fallback enabled for Retry - cause: " + exception.getMessage());
        return Collections.emptyList();
    }

    private List<Product> getProductsFallbackCircuitBreaker(RuntimeException exception) {
        log.info("resilience4j fallback enabled for Circuit-Breaker - cause: " + exception.getMessage());
        return Collections.emptyList();
    }

}
