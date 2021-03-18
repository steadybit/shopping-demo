# steadybit Demo Application

## 📝 Introduction

In order to give you a quick and easy start, we provide you a small demo application. Our shopping demo is a small product catalog provided by 4 distributed
services.

![Architecture](./architecture.jpg)

Each of the 3 backend services (`bestseller-fashion`, `bestseller-toys` and `hot-deals`) provides a list of products, which are aggregated by the `gateway` service and compiled in a
list. This list of products is accessible from `/products` at the `gateway` component.

Fallbacks, timeouts and circuit breakers are configured within the gateway service.
These resilience patterns have the task to mitigate failures or disturbances and let the `gateway` respond with defined fallbacks.

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

### Kubernetes

```sh
minikube start
minikube addons enable ingress
kubectl apply -f k8s-manifest.yml
open http://<minikube-ip>:8080
```


