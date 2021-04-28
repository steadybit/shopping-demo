import { ProductsLegacy } from './productsLegacy';
import { ProductsCircuitBreaker } from './productsCircuitBreaker';
import { ProductsParallel } from './productsParallel';

export const ProductService = {
    legacy: new ProductsLegacy(),
    parallel: new ProductsParallel(),
    circuitBreaker: new ProductsCircuitBreaker(),
};
