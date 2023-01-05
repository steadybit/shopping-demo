/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.1.1185 on 2023-01-05 12:54:40.

export interface Product {
    id: string;
    name: string;
    category: ProductCategory;
    imageId: string;
    price: number;
}

export interface Products {
    hotDeals: Product[];
    fashion: Product[];
    toys: Product[];
}

export interface ShoppingCart {
    id: string;
    items: ShoppingCartItem[];
}

export interface ShoppingCartItem {
    productId: string;
    quantity: number;
    price: number;
}

export type ProductCategory = 'FASHION' | 'TOYS';
