/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.shopping.domain;

import java.math.BigDecimal;

public class ProductBuilder {
    private String id;
    private String name;
    private ProductCategory category;
    private String imageId;
    private BigDecimal price;

    public ProductBuilder setId(String id) {
        this.id = id;
        return this;
    }

    public ProductBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public ProductBuilder setCategory(ProductCategory category) {
        this.category = category;
        return this;
    }

    public ProductBuilder setImageId(String imageId) {
        this.imageId = imageId;
        return this;
    }

    public ProductBuilder setPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public Product createProduct() {
        return new Product(id, name, category, imageId, price);
    }
}