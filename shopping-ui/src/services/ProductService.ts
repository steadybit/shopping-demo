import { ProductsLegacy } from './productsLegacy';
import { ProductsCircuitBreaker } from './productsCircuitBreaker';
import { ProductsParallel } from './productsParallel';
import { ProductsBasicExceptionHandling } from './productsBasicExceptionHandling';

export const ProductService = {
    legacy: new ProductsLegacy(),
    parallel: new ProductsParallel(),
    circuitBreaker: new ProductsCircuitBreaker(),
    basicExceptionHandling: new ProductsBasicExceptionHandling(),
};
