/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.gateway;

import com.steadybit.shopping.domain.Product;
import com.steadybit.shopping.domain.Products;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductsController {
    private static final Logger log = LoggerFactory.getLogger(GatewayApplication.class);
    private final RestTemplate restTemplateWithoutTimeout;
    private final RestTemplate restTemplate;
    private final WebClient webClient;
    private final ParameterizedTypeReference<Product> productTypeReference = new ParameterizedTypeReference<Product>() {
    };
    private final ParameterizedTypeReference<List<Product>> productListTypeReference = new ParameterizedTypeReference<List<Product>>() {
    };
    private final Resilience4jProductService resilience4jProductService;

    @Value("${rest.endpoint.fashion}")
    private String urlFashion;
    @Value("${rest.endpoint.toys}")
    private String urlToys;
    @Value("${rest.endpoint.hotdeals}")
    private String urlHotDeals;

    public ProductsController(RestTemplateBuilder restTemplateBuilder, WebClient webClient, Resilience4jProductService resilience4jProductService) {
        this.restTemplate = restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(2)).setReadTimeout(Duration.ofSeconds(2)).build();
        this.restTemplateWithoutTimeout = restTemplateBuilder.build();
        this.webClient = webClient;
        this.resilience4jProductService = resilience4jProductService;
    }

    @GetMapping
    public Products getProducts() {
        Products products = new Products();
        products.setFashion(this.getProduct(this.urlFashion));
        products.setToys(this.getProduct(this.urlToys));
        products.setHotDeals(this.getProduct(this.urlHotDeals));
        return products;
    }

    @GetMapping("/exception")
    public Products getProductsBasicExceptionHandling() {
        Products products = new Products();
        products.setFashion(this.getProductBasicExceptionHandling(this.urlFashion));
        products.setToys(this.getProductBasicExceptionHandling(this.urlToys));
        products.setHotDeals(this.getProductBasicExceptionHandling(this.urlHotDeals));
        return products;
    }

    @GetMapping("/timeout")
    public Products getProductsWithTimeout() {
        Products products = new Products();
        products.setFashion(this.getProductWithTimeout(this.urlFashion));
        products.setToys(this.getProductWithTimeout(this.urlToys));
        products.setHotDeals(this.getProductWithTimeout(this.urlHotDeals));
        return products;
    }

    @GetMapping("/retry")
    public Products getProductsWithResilience4JRetry() {
        Products products = new Products();
        products.setFashion(this.resilience4jProductService.getFashionWithRetry());
        products.setToys(this.resilience4jProductService.getToysWithRetry());
        products.setHotDeals(this.resilience4jProductService.getHotDealsWithRetry());
        return products;
    }

    @GetMapping({ "/circuitbreaker", "/cb", "/v2" })
    public Products getProductsWithResilience4JRetryAndCircuitBreaker() {
        Products products = new Products();
        products.setFashion(this.resilience4jProductService.getFashionWithRetryAndCircuitBreaker());
        products.setToys(this.resilience4jProductService.getToysWithRetryAndCircuitBreaker());
        products.setHotDeals(this.resilience4jProductService.getHotDealsWithRetryAndCircuitBreaker());
        return products;
    }

    @GetMapping("/parallel")
    public Mono<Products> getProductsParallel() {
        Mono<List<Product>> hotdeals = this.getProductReactive(this.urlHotDeals);
        Mono<List<Product>> fashion = this.getProductReactive(this.urlFashion);
        Mono<List<Product>> toys = this.getProductReactive(this.urlToys);

        return Mono.zip(hotdeals, fashion, toys)
                .flatMap(transformer -> Mono.just(new Products(transformer.getT1(), transformer.getT2(), transformer.getT3())));
    }

    private List<Product> getProduct(String url) {
        return this.restTemplateWithoutTimeout.exchange(url, HttpMethod.GET, null, this.productListTypeReference).getBody();
    }

    private List<Product> getProductBasicExceptionHandling(String url) {
        try {
            return this.getProduct(url);
        } catch (RestClientException e) {
            log.error("RestClientException occurred when fetching products", e);
            return Collections.emptyList();
        }
    }

    private List<Product> getProductWithTimeout(String url) {
        try {
            return this.restTemplate.exchange(url, HttpMethod.GET, null, this.productListTypeReference).getBody();
        } catch (RestClientException e) {
            log.error("RestClientException occurred when fetching products", e);
            return Collections.emptyList();
        }
    }

    private Mono<List<Product>> getProductReactive(String uri) {
        return this.webClient.get().uri(uri)
                .retrieve()
                .bodyToFlux(this.productTypeReference)
                .collectList()
                .flatMap(Mono::just)
                .doOnError(throwable -> log.error("Error occurred", throwable));
    }
}
