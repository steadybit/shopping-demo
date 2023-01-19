package com.steadybit.shopping.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ShoppingCart {
    private String id;
    private List<ShoppingCartItem> items;
}
