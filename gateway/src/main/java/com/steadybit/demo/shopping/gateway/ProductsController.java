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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductsController {

    private static final Logger log = LoggerFactory.getLogger(GatewayApplication.class);

    private final RestTemplate restTemplate;
    private final WebClient webClient;
    private final ParameterizedTypeReference<Product> productTypeReference = new ParameterizedTypeReference<Product>() {
    };
    private final ParameterizedTypeReference<List<Product>> productListTypeReference = new ParameterizedTypeReference<List<Product>>() {
    };

    @Value("${rest.endpoint.fashion}")
    private String urlFashion;
    @Value("${rest.endpoint.toys}")
    private String urlToys;
    @Value("${rest.endpoint.hotdeals}")
    private String urlHotDeals;

    public ProductsController(RestTemplate restTemplate, WebClient webClient) {
        this.restTemplate = restTemplate;
        this.webClient = webClient;
    }

    @GetMapping
    public Products getProducts() {
        Products products = new Products();
        products.setFashion(getProduct(urlFashion));
        products.setToys(getProduct(urlToys));
        products.setHotDeals(getProduct(urlHotDeals));
        return products;
    }

    @GetMapping({ "/circuitbreaker", "/cb", "/v2" })
    public Mono<Products> getProductsCircuitBreaker() {
        Mono<List<Product>> hotdeals = getProductCircuitBreaker("/products/hotdeals/circuitbreaker");
        Mono<List<Product>> fashion = getProductCircuitBreaker("/products/fashion/circuitbreaker");
        Mono<List<Product>> toys = getProductCircuitBreaker("/products/toys/circuitbreaker");

        return Mono.zip(hotdeals, fashion, toys)
                .flatMap(transformer -> Mono.just(new Products(transformer.getT1(), transformer.getT2(), transformer.getT3())));
    }

    @GetMapping("/fallback")
    public ResponseEntity<List<Product>> getProductsFallback() {
        log.info("fallback enabled");
        HttpHeaders headers = new HttpHeaders();
        headers.add("fallback", "true");
        return ResponseEntity.ok().headers(headers).body(Collections.emptyList());
    }

    private List<Product> getProduct(String url) {
        return restTemplate.exchange(url, HttpMethod.GET, null, productListTypeReference).getBody();
    }

    private Mono<List<Product>> getProductCircuitBreaker(String uri) {
        return webClient.get().uri(uri)
                .exchange()
                .flatMap(response -> response.bodyToFlux(productTypeReference)
                        .collectList()
                        .flatMap(Mono::just))
                .doOnError(throwable -> log.error("Error occured", throwable))
                .onErrorResume(throwable -> Mono.just(Collections.emptyList()));
    }
}
