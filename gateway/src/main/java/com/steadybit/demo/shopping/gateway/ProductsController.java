/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.gateway;

import com.steadybit.shopping.domain.Product;
import com.steadybit.shopping.domain.ProductResponse;
import com.steadybit.shopping.domain.Products;
import com.steadybit.shopping.domain.ResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
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

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Benjamin Wilms
 */
@RestController
public class ProductsController {

    public static final String URI_TOYS_BESTSELLER_CIRCUIT_BREAKER = "/cb/toys/bestseller";
    public static final String URI_FASHION_BESTSELLER_CIRCUIT_BREAKER = "/cb/fashion/bestseller";
    public static final String URI_HOTDEALS_CIRCUIT_BREAKER = "/cb/hotdeals";

    @Value("${rest.endpoint.fashion}")
    private String urlFashion;

    @Value("${rest.endpoint.toys}")
    private String urlToys;

    @Value("${rest.endpoint.hotdeals}")
    private String urlHotDeals;

    private RestTemplate restClient;

    private ProductResponse errorResponse;
    private WebClient webClient;

    private ParameterizedTypeReference<Product> productParameterizedTypeReference = new ParameterizedTypeReference<Product>() {
    };
    private Function<ClientResponse, Mono<ProductResponse>> responseProcessor = clientResponse -> {
        HttpHeaders headers = clientResponse.headers().asHttpHeaders();
        if (headers.containsKey("fallback") && headers.get("fallback").contains("true")) {
            return Mono.just(new ProductResponse(ResponseType.FALLBACK, Collections.emptyList()));
        } else if (clientResponse.statusCode().isError()) {
            // HTTP Error Codes are not handled by Hystrix!?
            return Mono.just(new ProductResponse(ResponseType.ERROR, Collections.emptyList()));
        }
        return clientResponse.bodyToFlux(productParameterizedTypeReference)
                .collectList()
                .flatMap(products -> Mono.just(new ProductResponse(ResponseType.REMOTE_SERVICE, products)));
    };

    public ProductsController(WebClient webClient) {
        this.webClient = webClient;
        this.restClient = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(1000))
                .setReadTimeout(Duration.ofMillis(1000))
                .build();
        this.errorResponse = new ProductResponse();
        errorResponse.setResponseType(ResponseType.ERROR);
        errorResponse.setProducts(Collections.emptyList());
    }

    @RequestMapping(value = { "/products", "/products/{version}" }, method = RequestMethod.GET)
    public Mono<Products> getProducts(@PathVariable Optional<String> version) {
        if (isCircuitBraker(version)) {
            return getProductsCircuitBreaker();
        }
        return getProductsLegacy();
    }

    @RequestMapping(value = { "/hotdeals", "/hotdeals/{version}" }, method = RequestMethod.GET)
    public Mono<ProductResponse> getHotDeals(@PathVariable Optional<String> version) {
        if (isCircuitBraker(version)) {
            return getProductCircuitBreaker(URI_HOTDEALS_CIRCUIT_BREAKER);
        }
        return Mono.just(getProductLegacy(urlHotDeals));
    }

    @RequestMapping(value = { "/fashion", "/fashion/{version}" }, method = RequestMethod.GET)
    public Mono<ProductResponse> getFashion(@PathVariable Optional<String> version) {
        if (isCircuitBraker(version)) {
            return getProductCircuitBreaker(URI_FASHION_BESTSELLER_CIRCUIT_BREAKER);
        }
        return Mono.just(getProductLegacy(urlFashion));
    }

    @RequestMapping(value = { "/toys", "/toys/{version}" }, method = RequestMethod.GET)
    public Mono<ProductResponse> getToys(@PathVariable Optional<String> version) {
        if (isCircuitBraker(version)) {
            return getProductCircuitBreaker(URI_TOYS_BESTSELLER_CIRCUIT_BREAKER);
        }
        return Mono.just(getProductLegacy(urlToys));
    }

    private boolean isCircuitBraker(@PathVariable Optional<String> version) {
        return version.map(v -> v.equalsIgnoreCase("cb") || v.equalsIgnoreCase("v2")).orElse(false);
    }

    private Mono<Products> getProductsLegacy() {
        Products products = new Products();
        long start = System.currentTimeMillis();

        products.setFashionResponse(getProductLegacy(urlFashion));
        products.setToysResponse(getProductLegacy(urlToys));
        products.setHotDealsResponse(getProductLegacy(urlHotDeals));

        products.setDuration(System.currentTimeMillis() - start);
        return Mono.just(products);

    }

    private Mono<Products> getProductsCircuitBreaker() {
        long start = System.currentTimeMillis();
        Mono<ProductResponse> hotdeals = getProductCircuitBreaker(URI_HOTDEALS_CIRCUIT_BREAKER);
        Mono<ProductResponse> fashionBestSellers = getProductCircuitBreaker(URI_FASHION_BESTSELLER_CIRCUIT_BREAKER);
        Mono<ProductResponse> toysBestSellers = getProductCircuitBreaker(URI_TOYS_BESTSELLER_CIRCUIT_BREAKER);
        return aggregateResults(start, hotdeals, fashionBestSellers, toysBestSellers);

    }

    private Mono<ProductResponse> getProductCircuitBreaker(String uri) {
        return webClient.get().uri(uri)
                .exchange()
                .flatMap(responseProcessor).doOnError(t -> {
                    System.out.println("on error");
                }).onErrorResume(t -> {
                    t.printStackTrace();
                    return Mono.just(errorResponse);
                });
    }

    private Mono<Products> aggregateResults(long start, Mono<ProductResponse> hotdeals, Mono<ProductResponse> fashionBestSellers,
            Mono<ProductResponse> toysBestSellers) {
        return Mono.zip(hotdeals, fashionBestSellers, toysBestSellers)
                .flatMap(transformer -> {
                    Products products = new Products();
                    products.setFashionResponse(transformer.getT2());
                    products.setHotDealsResponse(transformer.getT1());
                    products.setToysResponse(transformer.getT3());

                    products.setDuration(System.currentTimeMillis() - start);
                    return Mono.just(products);
                });
    }

    private ProductResponse getProductLegacy(String url) {
        ProductResponse response = new ProductResponse();

        response.setProducts(restClient.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<Product>>() {
        }).getBody());

        response.setResponseType(ResponseType.REMOTE_SERVICE);

        return response;
    }
}
