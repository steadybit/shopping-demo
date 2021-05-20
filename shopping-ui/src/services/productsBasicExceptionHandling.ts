import axios from 'axios';
import { Products } from '../../src-gen/ui-api';

export class ProductsBasicExceptionHandling {
    fetch = async (): Promise<Products> => {
        return (await axios.get('/products/exception')).data;
    };
}
