/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.shopping.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Benjamin Wilms
 */

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProductResponse {

    private ResponseType responseType;
    private List<Product> products;
}
