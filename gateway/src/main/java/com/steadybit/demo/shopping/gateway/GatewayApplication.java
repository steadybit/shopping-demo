/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class GatewayApplication implements WebFluxConfigurer {

    public static final String PATH_PRODUCTS = "/products";
    @Value("${rest.endpoint.fashion}")
    private String urlFashion;

    @Value("${rest.endpoint.toys}")
    private String urlToys;

    @Value("${rest.endpoint.hotdeals}")
    private String urlHotDeals;
    @Value("${rest.endpoint.checkout}")
    private String urlCheckout;

    @Value("${server.port}")
    private int serverPort;

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes().route("index", p -> p.path("/").uri("forward:/index.html"))//
                // Legacy routes
                .route("legacy-hotdeals", p -> p.path("/products/hotdeals**")//
                        .filters(c -> c.setPath(PATH_PRODUCTS))//
                        .uri(this.urlHotDeals))//
                .route("legacy-fashion", p -> p.path("/products/fashion**")
                        .filters(c -> c.setPath(PATH_PRODUCTS))//
                        .uri(this.urlFashion))//
                .route("legacy-toys", p -> p.path("/products/toys**")//
                        .filters(c -> c.setPath(PATH_PRODUCTS))//
                        .uri(this.urlToys))//
                // Circuit-Breaker routes
                .route("cb-hotdeals", p -> p.path("/products/hotdeals/circuitbreaker**")//
                        .filters(f -> f.retry(c -> c.setRetries(2).setSeries(HttpStatus.Series.SERVER_ERROR))//
                                .circuitBreaker(c -> c.setName("hotdeals").setFallbackUri("forward:/products/fallback"))
                                .setPath(PATH_PRODUCTS))//
                        .uri(this.urlHotDeals))//
                .route("cb-fashion", p -> p.path("/products/fashion/circuitbreaker**")//
                        .filters(f -> f.retry(c -> c.setRetries(2).setSeries(HttpStatus.Series.SERVER_ERROR))//
                                .circuitBreaker(c -> c.setName("fashion").setFallbackUri("forward:/products/fallback"))
                                .setPath(PATH_PRODUCTS))//
                        .uri(this.urlFashion))
                .route("cb-toys", p -> p.path("/products/toys/circuitbreaker**")//
                        .filters(f -> f.retry(c -> c.setRetries(2).setSeries(HttpStatus.Series.SERVER_ERROR))//
                                .circuitBreaker(c -> c.setName("toys").setFallbackUri("forward:/products/fallback"))
                                .setPath(PATH_PRODUCTS))//
                        .uri(this.urlToys))
                .route("checkout", p -> p.path("/checkout/**")
                        .uri(this.urlCheckout))
                .build();
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().baseUrl("http://localhost:" + this.serverPort).build();
    }

    @Bean
    public RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder();
    }

}
