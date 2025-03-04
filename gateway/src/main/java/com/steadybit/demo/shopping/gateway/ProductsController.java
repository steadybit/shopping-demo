/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.gateway;

import com.steadybit.shopping.domain.Product;
import com.steadybit.shopping.domain.Products;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductsController {
    private static final Logger log = LoggerFactory.getLogger(GatewayApplication.class);
    private final RestClient restClientNoTimeout;
    private final RestClient restClient;
    private final WebClient webClient;
    private final ParameterizedTypeReference<List<Product>> productListTypeReference = new ParameterizedTypeReference<List<Product>>() {
    };
    private final Resilience4jProductService resilience4jProductService;

    @Value("${rest.endpoint.fashion}")
    private String urlFashion;
    @Value("${rest.endpoint.toys}")
    private String urlToys;
    @Value("${rest.endpoint.hotdeals}")
    private String urlHotDeals;

    public ProductsController(WebClient webClient, Resilience4jProductService resilience4jProductService) {
        //we allow to set the connection close header, so we can disable the keep-alive
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "connection");

        var factoryNoTimeout = new JdkClientHttpRequestFactory(HttpClient.newBuilder().build());
        factoryNoTimeout.setReadTimeout((Duration) null);
        this.restClientNoTimeout = RestClient.builder().requestFactory(factoryNoTimeout).defaultHeader("Connection", "close").build();

        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = RestClient.builder().requestFactory(factory).defaultHeader("Connection", "close").build();
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

    @GetMapping({"/circuitbreaker", "/cb", "/v2"})
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
        return this.restClientNoTimeout.get().uri(url).retrieve().body(this.productListTypeReference);
    }

    private List<Product> getProductBasicExceptionHandling(String url) {
        try {
            return this.getProduct(url);
        } catch (RestClientException e) {
            log.error("RestClientException occurred when fetching products", e);
            return List.of();
        }
    }

    private List<Product> getProductWithTimeout(String url) {
        try {
            return restClient.get().uri(url).retrieve().body(this.productListTypeReference);
        } catch (RestClientException e) {
            log.error("RestClientException occurred when fetching products", e);
            return List.of();
        }
    }

    private Mono<List<Product>> getProductReactive(String uri) {
        return this.webClient.get().uri(uri)
                .retrieve()
                .bodyToFlux(Product.class)
                .collectList()
                .flatMap(Mono::just)
                .doOnError(throwable -> log.error("Error occurred", throwable));
    }
}
