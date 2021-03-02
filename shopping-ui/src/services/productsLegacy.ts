import axios from 'axios';
import {Products} from '../../src-gen/ui-api';


export class ProductsLegacy {
    fetch = async (): Promise<Products> => {
        return (await axios.get('/products')).data;
    };
}