# steadybit Demo Application

## 📝 Introduction

We provide a small demo application to give you a quick and easy start into the world of Chaos Engineering.
This demo application is a product catalog consisting of products from three different categories (toys, fashion and hot-deals).

## High Level Architecture

![Architecture](./architecture.jpg)

The shopping demo consists of three backend services per each product category (`bestseller-fashion`, `bestseller-toys` and `hot-deals`).
Each microservice provides a list of products.
These products are aggregated by the `gateway`-microservice and exposed to the user via `shopping-ui`.
In addition, each product microservice uses the `inventory-service` to determine stock availability.

## Technical Architecture

All services are based on Spring Boot and use different Spring projects.

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Cloud](https://spring.io/projects/spring-cloud)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)

As mentioned above the `gateway` is the entrypoint for the UI.

### Products REST Endpoint

The `gateway` provides available products via the `/products`-endpoint which collects all products from each
microservices (`bestseller-fashion`, `bestseller-toys` and `hot-deals`).
This endpoints is implemented in the [ProductsController](blob/master/gateway/src/main/java/com/steadybit/demo/shopping/gateway/ProductsController.java).
Multiple implementation strategies exists using each different resilience patterns: fallbacks, timeouts and circuit breakers as described subsequently.

There are multiple endpoints available to demonstrate different implementations.

| url                        | timeout configured |                       fallback-value                       |  retry configured  |  circuit breaker   | fails                                                                                                                                                                                                                                                                                                                                                                                                                             |
|----------------------------|:------------------:|:----------------------------------------------------------:|:------------------:|:------------------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `/products`                |        :x:         |                            :x:                             |        :x:         |        :x:         | - if a microservice is not reachable or returning an error: :red_circle: HTTP 500 <br>- if a microservice is not responding fast: :red_circle: the whole response will be delayed infinite                                                                                                                                                                                                                                        |
| `/products/exception`      |        :x:         |         :white_check_mark:<br>via catch exception          |        :x:         |        :x:         | - if a microservice is not reachable or returning an error: :white_check_mark: products of the category are omitted<br>- if a microservice is not responding fast: :red_circle: the whole response will be delayed infinite                                                                                                                                                                                                       |
| `/products/timeout`        | :white_check_mark: |         :white_check_mark:<br>via catch exception          |        :x:         |        :x:         | - if a microservice is not reachable or returning an error: :white_check_mark: products of the category are omitted<br>- if a microservice is not responding fast: :white_check_mark: products of the category are omitted                                                                                                                                                                                                        |
| `/products/retry`          | :white_check_mark: | :white_check_mark:<br>via `fallbackMethod` in resilience4j | :white_check_mark: |        :x:         | like `/products/timeout`, but with max 3 retries each 500ms if a microservice-request isn't successfull.<br>Pro:<br>- :+1: potential recovery from short-term problems<br>Con:<br>- :-1: increasing load on microservices<br>- :-1: increasing response time<br><br>There is also an [blog post](https://steadybit.com/blog/retries-with-resilience4j-and-how-to-check-in-your-real-world-environment) about this implementation. |
| `/products/circuitbreaker` | :white_check_mark: | :white_check_mark:<br>via `fallbackMethod` in resilience4j | :white_check_mark: | :white_check_mark: | like `/products/retry` but with a circuit breaker which is preventing a failing microservice from overload (also from retries) and allow it to recover                                                                                                                                                                                                                                                                            |
| `/products/parallel`       |   default (30s)    |                            :x:                             |        :x:         |        :x:         | Alternative implementation to show a parallelized way of fetching the products. This saves time, but the implementation has the same problems like the basic implementation.                                                                                                                                                                                                                                                      |

## Deploying the Application

Our demo can be run on different Docker based platforms using the deployment scripts provided.
Checkout the [Steadybit Quickstart](https://docs.steadybit.com/quick-start/deploy-example-application) for more details.

### Installation via helm

#### Using Helm in Kubernetes

```sh
helm repo add steadybit-shopping-demo https://steadybit.github.io/shopping-demo
helm repo update
helm upgrade steadybit-shopping-demo \
    --install \
    --wait \
    --timeout 5m0s \
    steadybit-shopping-demo/steadybit-shopping-demo
```
