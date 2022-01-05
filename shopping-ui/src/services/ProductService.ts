import { ProductsLegacy } from './productsLegacy';
import { ProductsCircuitBreaker } from './productsCircuitBreaker';
import { ProductsParallel } from './productsParallel';
import { ProductsExceptionHandling } from './productsExceptionHandling';
import { ProductsTimeoutHandling } from './productsTimeoutHandling';
import axios from 'axios';
import { ProductsResilience4j } from './productsResilience4j';

axios.defaults.timeout = 3000;

export const ProductService = {
    legacy: new ProductsLegacy(),
    parallel: new ProductsParallel(),
    circuitBreaker: new ProductsCircuitBreaker(),
    exceptionHandling: new ProductsExceptionHandling(),
    timeoutHandling: new ProductsTimeoutHandling(),
    resilience4j: new ProductsResilience4j(),
};
