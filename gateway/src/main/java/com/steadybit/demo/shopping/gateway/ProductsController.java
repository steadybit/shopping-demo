/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.gateway;

import com.steadybit.shopping.domain.Product;
import com.steadybit.shopping.domain.Products;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@RestController
public class ProductsController {

    private static final String URI_TOYS_BESTSELLER_CIRCUIT_BREAKER = "/cb/toys/bestseller";
    private static final String URI_FASHION_BESTSELLER_CIRCUIT_BREAKER = "/cb/fashion/bestseller";
    private static final String URI_HOTDEALS_CIRCUIT_BREAKER = "/cb/hotdeals";

    @Value("${rest.endpoint.fashion}")
    private String urlFashion;

    @Value("${rest.endpoint.toys}")
    private String urlToys;

    @Value("${rest.endpoint.hotdeals}")
    private String urlHotDeals;

    private RestTemplate restTemplate;

    private WebClient webClient;

    private ParameterizedTypeReference<Product> productTypeReference = new ParameterizedTypeReference<Product>() {
    };
    private ParameterizedTypeReference<List<Product>> productListTypeReference = new ParameterizedTypeReference<List<Product>>() {
    };
    private Function<ClientResponse, Mono<List<Product>>> responseProcessor = clientResponse -> {
        HttpHeaders headers = clientResponse.headers().asHttpHeaders();
        if (headers.containsKey("fallback") && headers.get("fallback").contains("true")) {
            return Mono.just(Collections.emptyList());
        }
        return clientResponse.bodyToFlux(productTypeReference)
                .collectList()
                .flatMap(products -> Mono.just(products));
    };

    public ProductsController(RestTemplate restTemplate, WebClient webClient) {
        this.restTemplate = restTemplate;
        this.webClient = webClient;
    }

    @RequestMapping(value = { "/products", "/products/{version}" }, method = RequestMethod.GET)
    public Mono<Products> getProducts(@PathVariable Optional<String> version) {
        if (isCircuitBraker(version)) {
            return getProductsCircuitBreaker();
        }
        return getProducts();
    }

    @RequestMapping(value = { "/hotdeals", "/hotdeals/{version}" }, method = RequestMethod.GET)
    public Mono<List<Product>> getHotDeals(@PathVariable Optional<String> version) {
        if (isCircuitBraker(version)) {
            return getProductCircuitBreaker(URI_HOTDEALS_CIRCUIT_BREAKER);
        }
        return Mono.just(getProduct(urlHotDeals));
    }

    @RequestMapping(value = { "/fashion", "/fashion/{version}" }, method = RequestMethod.GET)
    public Mono<List<Product>> getFashion(@PathVariable Optional<String> version) {
        if (isCircuitBraker(version)) {
            return getProductCircuitBreaker(URI_FASHION_BESTSELLER_CIRCUIT_BREAKER);
        }
        return Mono.just(getProduct(urlFashion));
    }

    @RequestMapping(value = { "/toys", "/toys/{version}" }, method = RequestMethod.GET)
    public Mono<List<Product>> getToys(@PathVariable Optional<String> version) {
        if (isCircuitBraker(version)) {
            return getProductCircuitBreaker(URI_TOYS_BESTSELLER_CIRCUIT_BREAKER);
        }
        return Mono.just(getProduct(urlToys));
    }

    private boolean isCircuitBraker(@PathVariable Optional<String> version) {
        return version.map(v -> v.equalsIgnoreCase("cb") || v.equalsIgnoreCase("v2")).orElse(false);
    }

    private Mono<Products> getProducts() {
        Products products = new Products();

        products.setFashion(getProduct(urlFashion));
        products.setToys(getProduct(urlToys));
        products.setHotDeals(getProduct(urlHotDeals));

        return Mono.just(products);

    }

    private List<Product> getProduct(String url) {
        return restTemplate.exchange(url, HttpMethod.GET, null, productListTypeReference).getBody();
    }

    private Mono<Products> getProductsCircuitBreaker() {
        Mono<List<Product>> hotdeals = getProductCircuitBreaker(URI_HOTDEALS_CIRCUIT_BREAKER);
        Mono<List<Product>> fashionBestSellers = getProductCircuitBreaker(URI_FASHION_BESTSELLER_CIRCUIT_BREAKER);
        Mono<List<Product>> toysBestSellers = getProductCircuitBreaker(URI_TOYS_BESTSELLER_CIRCUIT_BREAKER);

        return Mono.zip(hotdeals, fashionBestSellers, toysBestSellers)
                .flatMap(transformer -> Mono.just(new Products(transformer.getT1(), transformer.getT2(), transformer.getT3())));
    }

    private Mono<List<Product>> getProductCircuitBreaker(String uri) {
        return webClient.get().uri(uri)
                .exchange()
                .flatMap(responseProcessor).doOnError(t -> {
                    System.out.println("on error");
                }).onErrorResume(t -> {
                    t.printStackTrace();
                    return Mono.just(Collections.emptyList());
                });
    }
}
