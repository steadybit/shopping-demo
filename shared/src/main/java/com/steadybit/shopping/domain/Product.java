/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.shopping.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author Benjamin Wilms
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Product {

    private String id;
    private String name;
    private ProductCategory category;
    private String imageId;
    private BigDecimal price;

}
