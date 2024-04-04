import './Deal.scss';

import { Button, Card } from 'react-bootstrap';
import { ImgBeer, ImgCover, ImgHoodie, ImgKeychain, ImgPillow, ImgSocks, ImgSticker, ImgSunglasses } from '../../images';

import { Product } from '../../../src-gen/ui-api';
import React from 'react';
import classname from '../../utils/classname';

export type DealProps = {
    product?: Product;
    onAddToCart: () => void;
};

const block = classname('deal');

const Deal: React.FC<DealProps> = ({ product, onAddToCart }) => {
    if (!product) {
        return null;
    }
    return (
        <Card bg={product.availability ? product.availability.toLowerCase() : 'secondary'} text={'light'} className={block()}>
            <Card.Img variant="top" height={190} src={getImageSrc(product.imageId)} />
            <Card.Body>
                <div className={block('body')}>
                    <Card.Title className={block('title')}>{product.name}</Card.Title>
                    <Card.Text className={block('price')}>{product.price} $</Card.Text>
                </div>
                {product.availability !== 'UNAVAILABLE' ? (
                    <Button variant="primary" onClick={() => onAddToCart()}>
                        Add to Cart
                    </Button>
                ) : (
                    <Button variant="primary" disabled>
                        Out of Stock
                    </Button>
                )}
            </Card.Body>
        </Card>
    );
};

function getImageSrc(imageId: string) {
    switch (imageId) {
        case 'beer':
            return ImgBeer;
        case 'cover':
            return ImgCover;
        case 'hoodie':
            return ImgHoodie;
        case 'keychain':
            return ImgKeychain;
        case 'pillow':
            return ImgPillow;
        case 'socks':
            return ImgSocks;
        case 'sticker':
            return ImgSticker;
        case 'sunglasses':
            return ImgSunglasses;
        default:
            return 'about:blank';
    }
}

export default Deal;
