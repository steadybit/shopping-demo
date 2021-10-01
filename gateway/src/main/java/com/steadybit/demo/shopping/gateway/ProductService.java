/*
 * Copyright 2021 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.gateway;

import com.steadybit.shopping.domain.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final RestTemplate restTemplateWithoutTimeout;
    private final RestTemplate restTemplate;
    private final WebClient webClient;
    private final ParameterizedTypeReference<Product> productTypeReference = new ParameterizedTypeReference<Product>() {
    };
    private final ParameterizedTypeReference<List<Product>> productListTypeReference = new ParameterizedTypeReference<List<Product>>() {
    };

    public ProductService(RestTemplateBuilder restTemplateBuilder, WebClient webClient) {
        this.restTemplate = restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(2)).setReadTimeout(Duration.ofSeconds(2)).build();
        this.restTemplateWithoutTimeout = restTemplateBuilder.build();
        this.webClient = webClient;
    }

    public List<Product> getProduct(String url) {
        return this.restTemplateWithoutTimeout.exchange(url, HttpMethod.GET, null, this.productListTypeReference).getBody();
    }

    public List<Product> getProductBasicExceptionHandling(String url) {
        try {
            return this.getProduct(url);
        } catch (RestClientException e) {
            log.error("RestClientException occurred when fetching products", e);
            return Collections.emptyList();
        }
    }

    public List<Product> getProductWithTimeout(String url) {
        try {
            return this.restTemplate.exchange(url, HttpMethod.GET, null, this.productListTypeReference).getBody();
        } catch (RestClientException e) {
            log.error("RestClientException occurred when fetching products", e);
            return Collections.emptyList();
        }
    }

    public Mono<List<Product>> getProductReactive(String uri) {
        return this.webClient.get().uri(uri)
                .retrieve()
                .bodyToFlux(this.productTypeReference)
                .collectList()
                .flatMap(Mono::just)
                .doOnError(throwable -> log.error("Error occurred", throwable));
    }
}
