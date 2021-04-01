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
            <Route exact path={'/(V1)?'}>
                <HomeDeals fetchProducts={ProductService.legacy.fetch} version={'v1'} />
            </Route>
            <Route exact path={'/V2'}>
                <HomeDeals fetchProducts={ProductService.circuitBreaker.fetch} version={'v2'} />
            </Route>
        </Switch>
    </Router>;
};

const HomeDeals: React.FC<{ fetchProducts: () => Promise<Products>, version: 'v1' | 'v2' }> = ({ fetchProducts, version }) => {
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
            <div className={block(isLoading ? 'loading' : 'loading--hidden')}>
                <AiOutlineLoading />
            </div>
            <DropdownButton drop={'up'} variant='secondary' title={version.toUpperCase()} className={'version'}>
                {version === 'v1'
                    ? <Dropdown.Item href={'/#/V2'}>V2</Dropdown.Item>
                    : <Dropdown.Item href={'/#/V1'}>V1</Dropdown.Item>}
            </DropdownButton>
            <div className={block('lastUpdate')}>Last refresh: {timestamp.toLocaleTimeString()}</div>
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