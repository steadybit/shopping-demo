import React from 'react';
import { Button, Card } from 'react-bootstrap';
import { Product } from '../../../src-gen/ui-api';
import { ImgCar, ImgDrone, ImgExcavator, ImgJeans, ImgShirt, ImgSocks, ImgSunglasses, ImgTeddy } from '../../images';
import classname from '../../utils/classname';
import './Deal.scss';

export type DealProps = {
    product?: Product;
    onAddToCart: () => void;
};

const block = classname('deal');

const Deal: React.FC<DealProps> = ({ product, onAddToCart }) => {
    if (!product) {
        return null;
    }
    return <Card bg={product.availability ? product.availability.toLowerCase() : 'secondary'} text={'light'} className={block()}>
        <Card.Img variant='top' src={getImageSrc(product.imageId)} />
        <Card.Body>
            <div className={block('body')}>
                <Card.Title className={block('title')}>{product.name}</Card.Title>
                <Card.Text className={block('price')}>{product.price} $</Card.Text>
            </div>
            {
                product.availability !== 'UNAVAILABLE'
                    ? <Button variant='primary' onClick={() => onAddToCart()}>Add to Cart</Button>
                    : <Button variant='primary' disabled>Out of Stock</Button>
            }
        </Card.Body>
    </Card>;
};

function getImageSrc(imageId: string) {
    switch (imageId) {
        case 'car':
            return ImgCar;
        case 'drone':
            return ImgDrone;
        case 'excavator':
            return ImgExcavator;
        case 'jeans':
            return ImgJeans;
        case 'shirt':
            return ImgShirt;
        case 'socks':
            return ImgSocks;
        case 'sunglasses':
            return ImgSunglasses;
        case 'teddy':
            return ImgTeddy;
        default:
            return 'about:blank';
    }
}

export default Deal;
