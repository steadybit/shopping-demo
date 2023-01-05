import React from 'react';
import { Card } from 'react-bootstrap';
import { Product } from '../../../src-gen/ui-api';
import { ImgCar, ImgDrone, ImgExcavator, ImgJeans, ImgShirt, ImgSocks, ImgSunglasses, ImgTeddy } from '../../images';
import classname from '../../utils/classname';
import './Deal.scss';

export type DealProps = {
    product?: Product;
};

const block = classname('deal');

const Deal: React.FC<DealProps> = ({ product }) => {
    return product ? (
        <Card bg={product.availability ? product.availability.toLowerCase() : 'secondary'} text={'light'} className={block()}>
            <DealImage imageId={product.imageId} />
            <Card.Body className={block('body')}>
                <Card.Title className={block('title')}>{product.name}</Card.Title>
                <Card.Text className={block('price')}>{product.price} $</Card.Text>
            </Card.Body>
        </Card>
    ) : null;
};

const DealImage: React.FC<{ imageId: string }> = ({ imageId }) => {
    switch (imageId) {
        case 'car':
            return <Card.Img variant="top" src={ImgCar} />;
        case 'drone':
            return <Card.Img variant="top" src={ImgDrone} />;
        case 'excavator':
            return <Card.Img variant="top" src={ImgExcavator} />;
        case 'jeans':
            return <Card.Img variant="top" src={ImgJeans} />;
        case 'shirt':
            return <Card.Img variant="top" src={ImgShirt} />;
        case 'socks':
            return <Card.Img variant="top" src={ImgSocks} />;
        case 'sunglasses':
            return <Card.Img variant="top" src={ImgSunglasses} />;
        case 'teddy':
            return <Card.Img variant="top" src={ImgTeddy} />;
        default:
            return null;
    }
};

export default Deal;
