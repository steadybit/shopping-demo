package com.steadybit.demo.shopping.gateway;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;
import java.net.URISyntaxException;

@Controller
public class ProxyController {
    private final RestTemplate restTemplate;

    @Value("${rest.endpoint.checkout}")
    private String urlCheckout;

    @Value("${rest.endpoint.inventory}")
    private String urlInventory;


    public ProxyController(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @RequestMapping("/") public RedirectView indexRedirect() {
        return new RedirectView("/index.html");
    }

    @RequestMapping("/checkout/**")
    @ResponseBody
    public ResponseEntity<?> proxyCheckout(
            @RequestBody(required = false) String body,
            @RequestHeader HttpHeaders headers, HttpMethod method, HttpServletRequest request) throws URISyntaxException {
        return proxyRequest(body, headers, method, request, urlCheckout, "/checkout");
    }

    @RequestMapping("/inventory/**")
    @ResponseBody
    public ResponseEntity<?> proxyInventory(
            @RequestBody(required = false) String body,
            @RequestHeader HttpHeaders headers, HttpMethod method, HttpServletRequest request) throws URISyntaxException {
        return proxyRequest(body, headers, method, request, urlInventory, "/inventory");
    }

    private ResponseEntity<?> proxyRequest(String body, HttpHeaders headers, HttpMethod method, HttpServletRequest request, String baseUrl, String proxyPath)
            throws URISyntaxException {
        var path = request.getRequestURI();
        if (path.startsWith(proxyPath)) {
            path = path.substring(proxyPath.length());
        }
        if (path.startsWith("/") && baseUrl.endsWith("/")) {
            path = path.substring(1);
        }
        if (request.getQueryString()!=null) {
            path += "?" + request.getQueryString();
        }
        var uri = new URI(baseUrl + path);
        var httpEntity = new HttpEntity<>(body, headers);
        try {
            return restTemplate.exchange(uri, method, httpEntity, String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }
}