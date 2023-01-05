/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.bestseller.fashion;

import com.steadybit.shopping.domain.Product;
import com.steadybit.shopping.domain.ProductCategory;
import static java.util.UUID.randomUUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class BestsellerFashionApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BestsellerFashionApplication.class);

    @Autowired
    JdbcTemplate jdbcTemplate;

    public static void main(String[] args) {
        SpringApplication.run(BestsellerFashionApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("drop if exists table products");
        jdbcTemplate.execute("DROP TABLE IF EXISTS products_fashion");
        log.info("Creating table products");
        jdbcTemplate.execute(
                "CREATE TABLE products_fashion(" + "id VARCHAR(255), name VARCHAR(255), category VARCHAR(255), imageId VARCHAR(255), price DECIMAL(5,2))");
        List<Object[]> products = Stream.of(
                new Object[] { randomUUID(), "Bob Mailor Slim Jeans", ProductCategory.FASHION.toString(), "jeans", new BigDecimal("69.99") },
                new Object[] { randomUUID(), "Sun Glasses", ProductCategory.FASHION.toString(), "sunglasses",  new BigDecimal("49.99") },
                new Object[] { randomUUID(), "Urban Classics Shirt", ProductCategory.FASHION.toString(), "shirt", new BigDecimal("19.99") })
                .collect(Collectors.toList());
        jdbcTemplate.batchUpdate("INSERT INTO products_fashion(id, name, category, imageId, price) VALUES (?,?,?,?,?)", products);
        log.info("Data inserted");
        jdbcTemplate.query("SELECT id, name, category, imageId, price FROM products_fashion",
                (rs, rowNum) -> new Product(rs.getString("id"), rs.getString("name"), ProductCategory.valueOf(rs.getString("category")), rs.getString("imageId"), rs.getBigDecimal("price")))
                .forEach(product -> log.info(product.toString()));
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
