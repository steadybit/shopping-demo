import './Home.scss';

import { Card, Col, Container, Dropdown, DropdownButton, Row } from 'react-bootstrap';
import { CartView, useCart } from '../components/Cart/Cart';
import { Version, useProducts } from '../services/ProductService';

import { AiOutlineLoading } from 'react-icons/ai';
import Deal from '../components/Deal/Deal';
import { Product } from '../../src-gen/ui-api';
import React from 'react';
import classname from '../utils/classname';

const block = classname('home');
export const Home: React.FC<{ version?: Version }> = ({ version = 'simple' }) => {
    const products = useProducts(version);
    const [cart, addToCart, checkout] = useCart();

    return (
        <>
            <Container fluid={'xl'} className={block()}>
                <Row>
                    <Col xl={{ span: 3, order: 'last' }}>
                        <Container className={block('cart')}>
                            <CartView cart={cart} {...checkout} />
                        </Container>
                    </Col>
                    <Col>
                        {products.error ? (
                            <Error error={products.error.toString()} />
                        ) : (
                            <>
                                <Deals title={'Hot Deals'} products={products.content.hotDeals} onAddToCard={addToCart} />
                                <Deals title={'Fashion'} products={products.content.fashion} onAddToCard={addToCart} />
                                <Deals title={'Toys'} products={products.content.toys} onAddToCard={addToCart} />
                            </>
                        )}
                    </Col>
                </Row>
            </Container>
            <Debug version={version} loading={products.isLoading} timestamp={products.timestamp} />
        </>
    );
};

const Deals: React.FC<{ title: string; products: Product[]; onAddToCard: (product: Product) => void }> = ({ title, products, onAddToCard }) => {
    if (products?.length <= 0) {
        return null;
    }
    return (
        <Container className={block('deals')}>
            <Row>
                <h3>{title}</h3>
            </Row>
            <Row>
                {products.map((product) => (
                    <Col key={product.id} xs={4}>
                        <Deal key={product.id} product={product} onAddToCart={() => onAddToCard(product)} />
                    </Col>
                ))}
            </Row>
        </Container>
    );
};

const Error: React.FC<{ error: string }> = ({ error }) => {
    return (
        <Card bg={'danger'} text={'white'}>
            <Card.Header>Error: Could not load products</Card.Header>
            <Card.Body>
                <Card.Text>{error}</Card.Text>
            </Card.Body>
        </Card>
    );
};

const getEndpointName = function (version: Version = 'simple'): string {
    switch (version) {
        case 'retry':
            return 'with retry';
        case 'circuitBreaker':
            return 'with circuit breaker';
        case 'parallel':
            return 'with parallelization';
        case 'exception':
            return 'with exception handling';
        case 'timeout':
            return 'with timeout and exception handling';
        case 'simple':
            return 'as simple implementation';
    }
};

function Debug(props: { version?: Version; loading: boolean; timestamp: Date }) {
    return (
        <div className={block('debug')}>
            <div className={block('title')}>Endpoint</div>
            <DropdownButton size={'sm'} drop={'up'} variant="secondary" title={<span>{getEndpointName(props.version)}</span>} className={block('version')}>
                <Dropdown.Item href={'/#/retry'} active={props.version === 'retry'}>
                    {getEndpointName('retry')}
                </Dropdown.Item>
                <Dropdown.Item href={'/#/circuitBreaker'} active={props.version === 'circuitBreaker'}>
                    {getEndpointName('circuitBreaker')}
                </Dropdown.Item>
                <Dropdown.Item href={'/#/parallel'} active={props.version === 'parallel'}>
                    {getEndpointName('parallel')}
                </Dropdown.Item>
                <Dropdown.Item href={'/#/timeout'} active={props.version === 'timeout'}>
                    {getEndpointName('timeout')}
                </Dropdown.Item>
                <Dropdown.Item href={'/#/exception'} active={props.version === 'exception'}>
                    {getEndpointName('exception')}
                </Dropdown.Item>{' '}
                <Dropdown.Item href={'/#'} active={props.version === undefined}>
                    {getEndpointName(undefined)}
                </Dropdown.Item>
            </DropdownButton>
            <div className={block('lastUpdate')}>
                <div className={block('loading', props.loading ? [block('loading--hidden')] : [])}>
                    <AiOutlineLoading />
                </div>
                Last refresh: {props.timestamp.toLocaleTimeString()}
            </div>
        </div>
    );
}

export default Home;
