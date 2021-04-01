/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 2.30.840 on 2021-03-29 17:21:35.

export interface Product {
    id: string;
    name: string;
    category: ProductCategory;
    imageId: string;
    price: number;
}

export interface Products {
    fashion: Product[];
    toys: Product[];
    hotDeals: Product[];
}

export type ProductCategory = 'FASHION' | 'TOYS';
