/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.1.1185 on 2023-01-02 12:59:06.

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

export type ProductCategory = 'FASHION' | 'TOYS';
