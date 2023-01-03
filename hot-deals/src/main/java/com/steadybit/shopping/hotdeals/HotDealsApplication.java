/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.shopping.hotdeals;

import com.steadybit.shopping.domain.Product;
import com.steadybit.shopping.domain.ProductCategory;
import static java.util.UUID.randomUUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
public class HotDealsApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(HotDealsApplication.class);

    @Autowired
    JdbcTemplate jdbcTemplate;

    public static void main(String[] args) {
        SpringApplication.run(HotDealsApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("drop if exists table products");
        this.jdbcTemplate.execute("DROP TABLE IF EXISTS products_hotdeals");
        log.info("Creating table products");
        this.jdbcTemplate.execute(
                "CREATE TABLE products_hotdeals(" + "id VARCHAR(255), name VARCHAR(255), category VARCHAR(255), imageId VARCHAR(255), price DECIMAL(5,2))");
        List<Object[]> products = Stream.of(
                new Object[] { randomUUID(), "Socks Colourful Edition", ProductCategory.FASHION.toString(), "socks", new BigDecimal("19.99") },
                new Object[] { randomUUID(), "Quadcopter Drone", ProductCategory.TOYS.toString(), "drone", new BigDecimal("299.99") })
                .collect(Collectors.toList());
        this.jdbcTemplate.batchUpdate("INSERT INTO products_hotdeals(id, name, category, imageId, price) VALUES (?,?,?,?,?)", products);
        log.info("Data inserted");
        this.jdbcTemplate.query("SELECT id, name, category, imageId, price FROM products_hotdeals",
                (rs, rowNum) -> new Product(rs.getString("id"), rs.getString("name"), ProductCategory.valueOf(rs.getString("category")),
                        rs.getString("imageId"), rs.getBigDecimal("price")))
                .forEach(product -> log.info(product.toString()));
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
