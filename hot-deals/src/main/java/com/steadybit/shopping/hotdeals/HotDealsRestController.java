/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.shopping.hotdeals;

import com.steadybit.shopping.domain.Availability;
import com.steadybit.shopping.domain.Product;
import com.steadybit.shopping.domain.ProductCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/products")
public class HotDealsRestController {

    private static final Logger log = LoggerFactory.getLogger(HotDealsRestController.class);

    private final JdbcTemplate jdbcTemplate;
    private RestTemplate restTemplate;

    @Value("${rest.endpoint.inventory}")
    private String urlInventory;

    public HotDealsRestController(JdbcTemplate jdbcTemplate, RestTemplate restTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = restTemplate;
    }

    @GetMapping
    public List<Product> getHotDeals() {
        List<Product> products = this.jdbcTemplate.query("SELECT id, name, category, imageId, price FROM products_hotdeals",
                (rs, rowNum) -> new Product(rs.getString("id"), rs.getString("name"), ProductCategory.valueOf(rs.getString("category")),
                        rs.getString("imageId"), rs.getBigDecimal("price")));
        log.debug("Retrieving availability data for fashion bestsellers.");
        products.forEach(product -> {
            try {
                String urlTemplate = UriComponentsBuilder.fromHttpUrl(urlInventory)
                        .queryParam("id", product.getId()).encode().toUriString();
                Boolean isAvailable = restTemplate.getForObject(urlTemplate, Boolean.class);
                if (Boolean.TRUE.equals(isAvailable)) {
                    product.setAvailability(Availability.AVAILABLE);
                } else {
                    product.setAvailability(Availability.UNAVAILABLE);
                }
            } catch (RestClientException e) {
                product.setAvailability(Availability.UNKNOWN);
                log.warn("Unable to retrieve availability for product '" + product.getId() + "'.");
            }
        });
        return products;
    }

}
