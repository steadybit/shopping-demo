import {ProductsLegacy} from './productsLegacy';
import {ProductsCircuitBreaker} from './productsCircuitBreaker';

export const ProductService = {
    legacy: new ProductsLegacy(),
    circuitBreaker: new ProductsCircuitBreaker()
};