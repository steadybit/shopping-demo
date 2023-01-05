/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.1.1185 on 2023-01-05 13:31:55.

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

export type Availability = 'AVAILABLE' | 'UNAVAILABLE' | 'UNKNOWN';

export type ProductCategory = 'FASHION' | 'TOYS';
