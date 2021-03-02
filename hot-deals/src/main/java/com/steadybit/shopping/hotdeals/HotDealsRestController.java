/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.shopping.hotdeals;

import com.steadybit.shopping.domain.Product;
import com.steadybit.shopping.domain.ProductCategory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Benjamin Wilms
 */
@RestController
public class HotDealsRestController {

    private JdbcTemplate jdbcTemplate;

    public HotDealsRestController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/hotdeals")
    public List<Product> getHotDeals() {
        return jdbcTemplate.query("SELECT id, name, category, imageId, price FROM products_hotdeals",
                (rs, rowNum) -> new Product(rs.getString("id"), rs.getString("name"), ProductCategory.valueOf(rs.getString("category")), rs.getString("imageId"), rs.getBigDecimal("price")));
    }

}
