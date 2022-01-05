import axios from 'axios';
import { Products } from '../../src-gen/ui-api';

export class ProductsResilience4j {
    fetch = async (): Promise<Products> => {
        return (await axios.get('/products/resilience4j')).data;
    };
}
