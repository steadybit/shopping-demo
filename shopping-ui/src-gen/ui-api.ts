/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.1.1185 on 2023-01-24 16:17:07.

export interface Order {
    id: string;
    submitted: Date;
    items: OrderItem[];
}

export interface OrderItem {
    productId: string;
    quantity: number;
    price: number;
}

export interface Product {
    id: string;
    name: string;
    category: ProductCategory;
    imageId: string;
    price: number;
    availability: Availability;
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

export type Availability = 'AVAILABLE' | 'UNAVAILABLE' | 'UNKNOWN';

export type ProductCategory = 'FASHION' | 'TOYS';
