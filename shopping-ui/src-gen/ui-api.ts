/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 2.29.814 on 2021-03-02 11:22:41.

export interface Product {
    id: string;
    name: string;
    category: ProductCategory;
    imageId: string;
    price: number;
}

export interface ProductResponse {
    responseType: ResponseType;
    products: Product[];
}

export interface Products {
    fashionResponse: ProductResponse;
    toysResponse: ProductResponse;
    hotDealsResponse: ProductResponse;
    duration: number;
}

export type ProductCategory = 'FASHION' | 'TOYS' | 'BOOKS';

export type ResponseType = 'REMOTE_SERVICE' | 'SECOND_TRY' | 'FALLBACK' | 'ERROR';
