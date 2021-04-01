/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.gateway;

import com.steadybit.shopping.domain.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@SpringBootApplication
@RestController
@EnableDiscoveryClient
public class GatewayApplication implements WebFluxConfigurer {

    private static final Logger log = LoggerFactory.getLogger(GatewayApplication.class);

    @Value("${rest.endpoint.fashion}")
    private String urlFashion;

    @Value("${rest.endpoint.toys}")
    private String urlToys;

    @Value("${rest.endpoint.hotdeals}")
    private String urlHotDeals;

    @Value("${server.port}")
    private int serverPort;

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @GetMapping("/fallback")
    public ResponseEntity<List<Product>> fallback() {
        log.info("fallback enabled");
        HttpHeaders headers = new HttpHeaders();
        headers.add("fallback", "true");
        return ResponseEntity.ok().headers(headers).body(Collections.emptyList());
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes().route("index", p -> p.path("/").uri("forward:/index.html"))//
                // Legacy routes
                .route("legacy-hotdeals", p -> p.path("/hotdeals**")//
                        .uri(urlHotDeals))//
                .route("legacy-fashion", p -> p.path("/fashion/**")//
                        .uri(urlFashion))//
                .route("legacy-toys", p -> p.path("/toys/**")//
                        .uri(urlToys))//
                // Circuit-Breaker routes
                .route("cb-hotdeals", p -> p.path("/cb/hotdeals**")//
                        .filters(f -> f.retry(c -> c.setRetries(2).setSeries(HttpStatus.Series.SERVER_ERROR))//
                                .hystrix(c -> c.setName("hotdeals").setFallbackUri("forward:/fallback")).rewritePath("(\\/cb)", ""))//
                        .uri(urlHotDeals))//
                .route("cb-fashion", p -> p.path("/cb/fashion/**")//
                        .filters(f -> f.retry(c -> c.setRetries(2).setSeries(HttpStatus.Series.SERVER_ERROR))//
                                .hystrix(c -> c.setName("fashion").setFallbackUri("forward:/fallback")).rewritePath("(\\/cb)", "")).uri(urlFashion))
                .route("cb-toys", p -> p.path("/cb/toys/**")//
                        .filters(f -> f.retry(c -> c.setRetries(2).setSeries(HttpStatus.Series.SERVER_ERROR))//
                                .hystrix(c -> c.setName("toys").setFallbackUri("forward:/fallback")).rewritePath("(\\/cb)", ""))//
                        .uri(urlToys))
                .build();
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().baseUrl("http://localhost:" + serverPort).build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(2000))
                .setReadTimeout(Duration.ofMillis(2000))
                .build();
    }

}
