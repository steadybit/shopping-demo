/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.bestseller.fashion;

import com.steadybit.shopping.domain.Product;
import com.steadybit.shopping.domain.ProductCategory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Benjamin Wilms
 */
@RestController
@RequestMapping("/fashion")
public class BestsellerFashionRestController {

    private JdbcTemplate jdbcTemplate;

    public BestsellerFashionRestController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/bestseller")
    public List<Product> getBestsellerProducts() {
        return jdbcTemplate.query("SELECT id, name, category, imageId, price FROM products_fashion",
                (rs, rowNum) -> new Product(rs.getString("id"), rs.getString("name"), ProductCategory.valueOf(rs.getString("category")), rs.getString("imageId"), rs.getBigDecimal("price")));
    }

}
