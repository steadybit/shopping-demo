import axios from 'axios';
import { ShoppingCart } from '../../src-gen/ui-api';

axios.defaults.timeout = 3000;

export const CheckoutService = {
    direct: {
        async fetch(cart: ShoppingCart): Promise<void> {
            await axios.post('/checkout/direct', cart);
        }
    },
    buffered: {
        async fetch(cart: ShoppingCart): Promise<void> {
            await axios.post('/checkout/buffered', cart);
        }
    }
};
