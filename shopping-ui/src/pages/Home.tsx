import React from 'react';
import { Card, Col, Container, Dropdown, DropdownButton, Jumbotron, Row } from 'react-bootstrap';
import Deal from '../components/Deal/Deal';
import useAsync from '../utils/useAsync';
import { Product, Products } from '../../src-gen/ui-api';
import classname from '../utils/classname';
import { HashRouter as Router, Route, Switch } from 'react-router-dom';
import { ProductService } from '../services/ProductService';
import './Home.scss';
import { AiOutlineLoading } from 'react-icons/all';

const EmptyStartpage: Products = {
    fashion: [],
    toys: [],
    hotDeals: []
};

const block = classname('home');
const Home: React.FC = () => {
    return <Router>
        <Switch>
            <Route exact path={'/'}>
                <HomeDeals fetchProducts={ProductService.legacy.fetch} />
            </Route>
            <Route exact path={'/circuitbreaker'}>
                <HomeDeals fetchProducts={ProductService.circuitBreaker.fetch} version={'circuitBreaker'} />
            </Route>
        </Switch>
    </Router>;
};

const HomeDeals: React.FC<{ fetchProducts: () => Promise<Products>, version?: undefined | 'circuitBreaker' }> = ({ fetchProducts, version }) => {
    const [timestamp, setTimestamp] = React.useState(new Date());
    const [products, { error, isLoading }] = useAsync(EmptyStartpage, () => fetchProducts(), [timestamp]);
    React.useEffect(() => {
        const handle = setInterval(() => {
            if (!isLoading) {
                setTimestamp(new Date());
            }
        }, 1000);
        return () => {
            clearInterval(handle);
        };

    });

    return <Container className={block()}>
        {error
            ? <Error error={error.toString()} />
            : <>
                <Deals title={'Hot Deals'} products={products.hotDeals} />
                <Deals title={'Fashion'} products={products.fashion} />
                <Deals title={'Toys'} products={products.toys} />
            </>}
        <div className={block('debug')}>
            <div className={block('title')}>Endpoint</div>
            <DropdownButton size={'sm'} drop={'up'} variant='secondary'
                            title={<span>{version === 'circuitBreaker' ? 'with Circuit Breaker' : 'without Circuit Breaker'}</span>}
                            className={block('version')}>
                <Dropdown.Item href={'/#/circuitBreaker'} active={version === 'circuitBreaker'}>with Circuit Breaker</Dropdown.Item>
                <Dropdown.Item href={'/#'} active={version === undefined}>
                    without Circuit Breaker
                </Dropdown.Item>
            </DropdownButton>
            <div className={block('lastUpdate')}>
                <div className={block('loading', isLoading ? [block('loading--hidden')] : [])}>
                    <AiOutlineLoading />
                </div>
                Last refresh: {timestamp.toLocaleTimeString()}</div>
        </div>
    </Container>;
};

const Deals: React.FC<{ title: string, products: Product[] }> = ({ title, products }) => {
    return products?.length > 0
        ? <Jumbotron className={block('deals')}>
            <Container>
                <Row>
                    <h3>{title}</h3>
                </Row>
                <Row>
                    <Col xs={4}><Deal product={products?.[0]} /></Col>
                    <Col xs={4}><Deal product={products?.[1]} /></Col>
                    <Col xs={4}><Deal product={products?.[2]} /></Col>
                </Row>
            </Container>
        </Jumbotron>
        : null;

};

const Error: React.FC<{ error: string }> = ({ error }) => {
    return <Card bg={'danger'} text={'white'}
    >
        <Card.Header>Error: Could not load products</Card.Header>
        <Card.Body>
            <Card.Text>
                {error}
            </Card.Text>
        </Card.Body>
    </Card>;
};
export default Home;