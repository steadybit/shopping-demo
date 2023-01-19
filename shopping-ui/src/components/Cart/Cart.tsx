import './Cart.scss';
import React, { useCallback, useState } from 'react';
import { Product, ShoppingCart } from '../../../src-gen/ui-api';
import structuredClone from '@ungap/structured-clone';
import { v4 as uuidv4 } from 'uuid';
import { Alert, Button, Card, Dropdown, DropdownButton, ListGroup, Stack } from 'react-bootstrap';
import classname from '../../utils/classname';
import { CheckoutService } from '../../services/CheckoutService';
import { AiOutlineLoading } from 'react-icons/ai';

export type Cart = {
    items: CartItem[];
}

export  type CartItem = {
    product: Product;
    quantity: number;
}

export type CartProps = {
    cart: Cart;
    isLoading?: boolean;
    success?: boolean;
    error?: unknown;
    onCheckout: (type: string) => void;
};

const block = classname('cart');

export const CartView: React.FC<CartProps> = ({ cart, isLoading, error, success, onCheckout }) => {
    const [type, setType] = React.useState('direct send');

    return <Card className={block()}>
        <Card.Body>
            <Card.Title>Cart</Card.Title>
            <Stack gap={3}>
                {
                    cart.items.length === 0 ? <Card.Text className='text-muted'>Empty</Card.Text> : <ListGroup variant='flush'>
                        {cart.items.map(item => <ListGroup.Item key={item.product.id}>
                            {item.product.name} x {item.quantity}
                        </ListGroup.Item>)}
                    </ListGroup>
                }
                <Stack direction='horizontal' gap={3}>
                    <Button variant='primary' disabled={cart.items.length === 0 || isLoading} onClick={() => onCheckout(type)}>
                        Checkout
                    </Button>
                    <DropdownButton variant='light' title={type}>
                        <Dropdown.Item active={type === 'direct send'} onClick={() => setType('direct send')}>
                            direct send
                        </Dropdown.Item>
                        <Dropdown.Item active={type === 'buffered send'} onClick={() => setType('buffered send')}>
                            buffered send
                        </Dropdown.Item>
                    </DropdownButton>
                    {isLoading ? <div className={block('loading')}>
                        <AiOutlineLoading />
                    </div> : null}
                </Stack>
                {error ? <Alert variant={'danger'}>
                    {error.toString()}
                </Alert> : success ? <Alert variant={'success'}>
                    Success
                </Alert> : null}
            </Stack>
        </Card.Body>
    </Card>;
};

type Checkout = {
    isLoading?: boolean;
    success?: boolean;
    error?: unknown;
    onCheckout: (type: string) => void;
}

export function useCart(): [Cart, (product: Product) => void, Checkout] {
    const [cart, addToCart] = React.useReducer((state: Cart, product: Product) => {
        const newState = structuredClone(state);
        const item = newState.items.find(item => item.product.id === product.id);
        if (item) {
            item.quantity++;
        } else {
            newState.items.push({ product, quantity: 1 });
        }
        return newState;
    }, { items: [] });


    const [checkout, setCheckout] = useState<Omit<Checkout, 'onCheckout'>>({
        error: undefined,
        isLoading: false,
        success: false
    });

    const onCheckout = useCallback(async (type: string) => {
        const shoppingcart: ShoppingCart = {
            id: uuidv4(),
            items: cart.items.map(item => ({
                productId: item.product.id,
                quantity: item.quantity,
                price: item.product.price
            }))
        };

        try {
            setCheckout((s) => ({ ...s, isLoading: true, error: undefined, success: false }));
            switch (type) {
                case 'buffered send':
                    await CheckoutService.buffered.fetch(shoppingcart);
                    break;
                default:
                    await CheckoutService.direct.fetch(shoppingcart);
                    break;
            }
            setCheckout((s) => ({ ...s, isLoading: false, error: undefined, success: true }));
        } catch (error) {
            setCheckout((s) => ({ ...s, isLoading: false, error, success: false }));
        }
    }, [cart]);

    return [
        cart,
        addToCart,
        {
            ...checkout,
            onCheckout
        }
    ];

}