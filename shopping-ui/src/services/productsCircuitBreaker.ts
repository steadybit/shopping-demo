import axios from 'axios';
import {Products} from '../../src-gen/ui-api';


export class ProductsCircuitBreaker {
    fetch = async (): Promise<Products> => {
        return (await axios.get('/products/circuitbreaker')).data;
    };
}
