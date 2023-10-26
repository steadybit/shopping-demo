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
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    private final RestTemplate restTemplate;
    private final ParameterizedTypeReference<List<Product>> productListTypeReference = new ParameterizedTypeReference<List<Product>>() {
    };

    public Resilience4jProductService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(2)).setReadTimeout(Duration.ofSeconds(2)).build();
    }

    @Retry(name = "fashion", fallbackMethod = "getProductsFallbackRetry")
    public List<Product> getFashionWithRetry() {
        return this.restTemplate.exchange(this.urlFashion, HttpMethod.GET, null, this.productListTypeReference).getBody();
    }

    @Retry(name = "toys", fallbackMethod = "getProductsFallbackRetry")
    public List<Product> getToysWithRetry() {
        return this.restTemplate.exchange(this.urlToys, HttpMethod.GET, null, this.productListTypeReference).getBody();
    }

    @Retry(name = "hotdeals", fallbackMethod = "getProductsFallbackRetry")
    public List<Product> getHotDealsWithRetry() {
        return this.restTemplate.exchange(this.urlHotDeals, HttpMethod.GET, null, this.productListTypeReference).getBody();
    }

    @Retry(name = "fashion", fallbackMethod = "getProductsFallbackRetry")
    @CircuitBreaker(name = "fashion", fallbackMethod = "getProductsFallbackCircuitBreaker")
    public List<Product> getFashionWithRetryAndCircuitBreaker() {
        return this.restTemplate.exchange(this.urlFashion, HttpMethod.GET, null, this.productListTypeReference).getBody();
    }

    @Retry(name = "toys", fallbackMethod = "getProductsFallbackRetry")
    @CircuitBreaker(name = "toys", fallbackMethod = "getProductsFallbackCircuitBreaker")
    public List<Product> getToysWithRetryAndCircuitBreaker() {
        return this.restTemplate.exchange(this.urlToys, HttpMethod.GET, null, this.productListTypeReference).getBody();
    }

    @Retry(name = "hotdeals", fallbackMethod = "getProductsFallbackRetry")
    @CircuitBreaker(name = "hotdeals", fallbackMethod = "getProductsFallbackCircuitBreaker")
    public List<Product> getHotDealsWithRetryAndCircuitBreaker() {
        return this.restTemplate.exchange(this.urlHotDeals, HttpMethod.GET, null, this.productListTypeReference).getBody();
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
