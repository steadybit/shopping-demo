import axios from 'axios';
import { Products } from '../../src-gen/ui-api';

export class ProductsTimeoutHandling {
    fetch = async (): Promise<Products> => {
        return (await axios.get('/products/timeout')).data;
    };
}
