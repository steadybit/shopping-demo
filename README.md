# steadybit Demo Application

## 📝 Introduction

In order to give you a quick and easy start, we provide you a small demo application. Our shopping demo is a small product catalog provided by 4 distributed
services (three backend microservice and one frontend).

![Architecture](./architecture.jpg)

Each of the 3 backend services (`bestseller-fashion`, `bestseller-toys` and `hot-deals`) provides a list of products, which are aggregated by the `gateway`
service and compiled in a list. This list of products is accessible from `/products` at the `gateway` component.

Fallbacks, timeouts and circuit breakers are configured within the `gateway` service. These resilience patterns have the task to mitigate failures or
disturbances and let the `gateway` respond with defined fallbacks. See more about that in the section below, called "Implementation".

## Stack

All services are based on Spring Boot and use different Spring projects.

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Cloud](https://spring.io/projects/spring-cloud)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Spring Cloud Netflix](https://spring.io/projects/spring-cloud-netflix)
- [Spring Cloud Circuit Breaker](https://spring.io/projects/spring-cloud-circuitbreaker)
- [Spring Cloud Kubernetes](https://spring.io/projects/spring-cloud-kubernetes)

## 🚀 Getting started

Our demo can be run on different Docker based platforms using the deployment scripts provided. Following types of deployments on the corresponding platforms are
prepared.

### Docker

```sh
docker-compose up
open http://localhost:8080
```

### Kubernetes (local)

```sh
minikube start
minikube addons enable ingress
kubectl apply -f k8s-manifest.yml
kubectl apply -f k8s-manifest-minikube-ingress.yml
open http://<minikube-ip>:8080
```

### Kubernetes (AWS EKS)

Install `eksctl` CLI

```sh
brew tap weaveworks/tap
brew install weaveworks/tap/eksctl
```

Create a new cluster in:

```sh
eksctl create cluster -f aws-eks-kubernetes/aws-eks-demo.yaml --alb-ingress-access
```

Deploy demo application:

```sh
kubectl apply -f k8s-manifest.yml
kubectl apply -f k8s-manifest-eks-ingress.yml
```

## Implementation

As mentioned above the `gateway` is the entrypoint for the UI. It is based on Spring Boot and Spring Cloud projects. We will cover its most important parts in
this section

### Products REST Endpoint - Basic Implementation

The `gateway` provides basically one endpoint which collects products from different categories (hot-deals, fashion and toys) from the corresponding
microservices (`bestseller-fashion`, `bestseller-toys` and `hot-deals`). This endpoint is implemented in
the [ProductsController](blob/master/gateway/src/main/java/com/steadybit/demo/shopping/gateway/ProductsController.java). Below are the relevant parts for the
basic implementation of this endpoint.

```java

@RestController
public class ProductsController {
    @Value("${rest.endpoint.hotdeals}")
    private String urlHotDeals;

    private RestTemplate restTemplate;
    //...

    @RequestMapping(value = { "/products" }, method = RequestMethod.GET)
    public Products getProducts() {
        Products products = new Products();
        products.setFashion(getProduct(urlFashion));
        products.setToys(getProduct(urlToys));
        products.setHotDeals(getProduct(urlHotDeals));
        return products;
    }

    private List<Product> getProduct(String url) {
        return restTemplate.exchange(url, HttpMethod.GET, null, productListTypeReference).getBody();
    }
}
```

So, whenever the UI requests an update by calling `/products`-Endpoint the `gateway` microservices collects synchronously the products from each microservice.

### Products REST Endpoint - Circuit Breaker with Fallback

Besides the basic implementation described above, there is also a REST Endpoint using an implemented Circuit Breaker (
via [Hystrix](https://www.baeldung.com/spring-cloud-netflix-hystrix)). In order to reach that version of the endpoint, the `gateway`'
s [ProductsController](blob/master/gateway/src/main/java/com/steadybit/demo/shopping/gateway/ProductsController.java) is called via `/products/circuitbreaker`.
Whenever the corresponding product-microservice (e.g. `hot-deals`) is not reachable the `/products/fallback` will provide an empty list as an alternative
response. This way, the UI is simply not showing products from this category but can still show results of the other microservices (e.g `fashion`, `toys`).

```java

@RestController
@RequestMapping("/products")
public class ProductsController {
    // ...
    @GetMapping("/circuitbreaker")
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

    private Mono<List<Product>> getProductCircuitBreaker(String uri) {
        return webClient.get().uri(uri)
                .exchange()
                .flatMap(response -> response.bodyToFlux(productTypeReference)
                        .collectList()
                        .flatMap(Mono::just))
                .doOnError(throwable -> log.error("Error occured", throwable));
    }
}
```

The routing of the Circuit Breaker is implemented in the Main Class of
the `gateway`: [GatewayApplication](blob/master/gateway/src/main/java/com/steadybit/demo/shopping/gateway/GatewayApplication.java).

````java

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication implements WebFluxConfigurer {

    @Value("${rest.endpoint.hotdeals}")
    private String urlHotDeals;

    //...
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                // ...
                .route("cb-hotdeals", p -> p.path("/products/hotdeals/circuitbreaker**")//
                        .filters(f -> f.retry(c -> c.setRetries(2).setSeries(HttpStatus.Series.SERVER_ERROR))//
                                .hystrix(c -> c.setName("hotdeals").setFallbackUri("forward:/products/fallback"))
                                .setPath("/products"))//
                        .uri(urlHotDeals))//
                .build();
    }

}
````

### Additional REST Endpoints

There are some additional endpoints which are helpful for HTTP Health Schecks during experiments, such as:

- `/product/hotdeals` to reach `hot-deals` products-list and check it's availability
- `/product/fashion` to reach `fashion` products-list and check it's availability
- `/product/toys` to reach `toys` products-list and check it's availability