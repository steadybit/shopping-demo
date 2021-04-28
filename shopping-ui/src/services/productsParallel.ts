import axios from 'axios';
import { Products } from '../../src-gen/ui-api';

export class ProductsParallel {
    fetch = async (): Promise<Products> => {
        return (await axios.get('/products/parallel')).data;
    };
}
