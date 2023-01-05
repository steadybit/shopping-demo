import { ProductsLegacy } from './productsLegacy';
import { ProductsCircuitBreaker } from './productsCircuitBreaker';
import { ProductsParallel } from './productsParallel';
import { ProductsExceptionHandling } from './productsExceptionHandling';
import { ProductsTimeoutHandling } from './productsTimeoutHandling';
import axios from 'axios';

axios.defaults.timeout = 3000;

export const ProductService = {
    legacy: new ProductsLegacy(),
    parallel: new ProductsParallel(),
    circuitBreaker: new ProductsCircuitBreaker(),
    exceptionHandling: new ProductsExceptionHandling(),
    timeoutHandling: new ProductsTimeoutHandling(),
};
