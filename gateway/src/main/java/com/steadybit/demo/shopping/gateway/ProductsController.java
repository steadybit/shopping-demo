/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.gateway;

import com.steadybit.shopping.domain.Product;
import com.steadybit.shopping.domain.Products;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductsController {
    private static final Logger log = LoggerFactory.getLogger(GatewayApplication.class);

    private final ProductService productService;

    @Value("${rest.endpoint.fashion}")
    private String urlFashion;
    @Value("${rest.endpoint.toys}")
    private String urlToys;
    @Value("${rest.endpoint.hotdeals}")
    private String urlHotDeals;

    public ProductsController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Products getProducts() {
        Products products = new Products();
        products.setFashion(this.productService.getProduct(this.urlFashion));
        products.setToys(this.productService.getProduct(this.urlToys));
        products.setHotDeals(this.productService.getProduct(this.urlHotDeals));
        return products;
    }

    @GetMapping("/exception")
    public Products getProductsBasicExceptionHandling() {
        Products products = new Products();
        products.setFashion(this.productService.getProductBasicExceptionHandling(this.urlFashion));
        products.setToys(this.productService.getProductBasicExceptionHandling(this.urlToys));
        products.setHotDeals(this.productService.getProductBasicExceptionHandling(this.urlHotDeals));
        return products;
    }

    @GetMapping("/resilience4j")
    public Products getProductsResilience4j() {
        Products products = new Products();
        products.setFashion(this.productService.getProductsResilience4j(this.urlFashion));
        products.setToys(this.productService.getProductsResilience4j(this.urlToys));
        products.setHotDeals(this.productService.getProductsResilience4j(this.urlHotDeals));
        return products;
    }

    @GetMapping("/timeout")
    public Products getProductsWithTimeout() {
        Products products = new Products();
        products.setFashion(this.productService.getProductWithTimeout(this.urlFashion));
        products.setToys(this.productService.getProductWithTimeout(this.urlToys));
        products.setHotDeals(this.productService.getProductWithTimeout(this.urlHotDeals));
        return products;
    }

    @GetMapping("/parallel")
    public Mono<Products> getProductsParallel() {
        Mono<List<Product>> hotdeals = this.productService.getProductReactive("/products/hotdeals");
        Mono<List<Product>> fashion = this.productService.getProductReactive("/products/fashion");
        Mono<List<Product>> toys = this.productService.getProductReactive("/products/toys");

        return Mono.zip(hotdeals, fashion, toys)
                .flatMap(transformer -> Mono.just(new Products(transformer.getT1(), transformer.getT2(), transformer.getT3())));
    }

    @GetMapping({ "/circuitbreaker", "/cb", "/v2" })
    public Mono<Products> getProductsCircuitBreaker() {
        Mono<List<Product>> hotdeals = this.productService.getProductReactive("/products/hotdeals/circuitbreaker");
        Mono<List<Product>> fashion = this.productService.getProductReactive("/products/fashion/circuitbreaker");
        Mono<List<Product>> toys = this.productService.getProductReactive("/products/toys/circuitbreaker");

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

}
