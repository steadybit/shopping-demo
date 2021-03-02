/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.shopping.domain;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Benjamin Wilms
 */

@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonPropertyOrder({ "fashionResponse", "toysResponse", "hotDealsResponse", "duration"})
public class Products {

    private long duration;
    private ProductResponse fashionResponse;
    private ProductResponse toysResponse;
    private ProductResponse hotDealsResponse;
}
