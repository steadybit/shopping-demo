/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.shopping.domain;

import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class Product {

    @NonNull
    private String id;
    @NonNull
    private String name;
    @NonNull
    private ProductCategory category;
    @NonNull
    private String imageId;
    @NonNull
    private BigDecimal price;
    private Availability availability;

}
