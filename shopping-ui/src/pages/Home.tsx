import React, { useMemo } from 'react';
import { Card, Col, Container, Dropdown, DropdownButton, Row } from 'react-bootstrap';
import Deal from '../components/Deal/Deal';
import useAsync from '../utils/useAsync';
import { Product, Products } from '../../src-gen/ui-api';
import classname from '../utils/classname';
import { HashRouter as Router, Route, Switch } from 'react-router-dom';
import { ProductService } from '../services/ProductService';
import './Home.scss';
import { AiOutlineLoading } from 'react-icons/ai';

const EmptyStartpage: Products = {
    fashion: [],
    toys: [],
    hotDeals: []
};

const block = classname('home');
const Home: React.FC = () => {
    return (
        <Router>
            <Switch>
                <Route path={'/:version?'} >
                    {({ match }) => (
                        <HomeDeals version={match?.params.version as Version} />
                    )}
                </Route>
            </Switch>
        </Router>
    );
};

type Version = 'simple' | 'timeout' | 'exception' | 'parallel' | 'circuitBreaker';

const getEndpointName = function(version: Version = 'simple'): string {
    switch (version) {
        case 'circuitBreaker':
            return 'with Circuit Breaker';
        case 'parallel':
            return 'with parallelization';
        case 'exception':
            return 'with exception handling';
        case 'timeout':
            return 'with timeout and exception handling';
        default:
            return 'as simple implementation';
    }
};
const HomeDeals: React.FC<{ version?: Version }> = ({ version }) => {
    const [timestamp, setTimestamp] = React.useState(new Date());
    const fetchProducts = useMemo(() => {
        switch (version) {
            case 'timeout':
                return ProductService.timeoutHandling.fetch;
            case 'exception':
                return ProductService.exceptionHandling.fetch;
            case 'parallel':
                return ProductService.parallel.fetch;
            case 'circuitBreaker':
                return ProductService.circuitBreaker.fetch;
            default:
                return ProductService.legacy.fetch;
        }
    }, [version]);
    console.log(version);
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

    return (
        <Container className={block()}>
            {error ? (
                <Error error={error.toString()} />
            ) : (
                <>
                    <Deals title={'Hot Deals'} products={products.hotDeals} />
                    <Deals title={'Fashion'} products={products.fashion} />
                    <Deals title={'Toys'} products={products.toys} />
                </>
            )}
            <div className={block('debug')}>
                <div className={block('title')}>Endpoint</div>
                <DropdownButton size={'sm'} drop={'up'} variant='secondary' title={<span>{getEndpointName(version)}</span>} className={block('version')}>
                    <Dropdown.Item href={'/#/circuitBreaker'} active={version === 'circuitBreaker'}>
                        {getEndpointName('circuitBreaker')}
                    </Dropdown.Item>
                    <Dropdown.Item href={'/#/parallel'} active={version === 'parallel'}>
                        {getEndpointName('parallel')}
                    </Dropdown.Item>
                    <Dropdown.Item href={'/#/timeout'} active={version === 'timeout'}>
                        {getEndpointName('timeout')}
                    </Dropdown.Item>
                    <Dropdown.Item href={'/#/exception'} active={version === 'exception'}>
                        {getEndpointName('exception')}
                    </Dropdown.Item>{' '}
                    <Dropdown.Item href={'/#'} active={version === undefined}>
                        {getEndpointName(undefined)}
                    </Dropdown.Item>
                </DropdownButton>
                <div className={block('lastUpdate')}>
                    <div className={block('loading', isLoading ? [block('loading--hidden')] : [])}>
                        <AiOutlineLoading />
                    </div>
                    Last refresh: {timestamp.toLocaleTimeString()}
                </div>
            </div>
        </Container>
    );
};

const Deals: React.FC<{ title: string; products: Product[] }> = ({ title, products }) => {
    return products?.length > 0 ? (
        <div className={block('deals')}>
            <Container>
                <Row>
                    <h3>{title}</h3>
                </Row>
                <Row>
                    <Col xs={4}>
                        <Deal product={products?.[0]} />
                    </Col>
                    <Col xs={4}>
                        <Deal product={products?.[1]} />
                    </Col>
                    <Col xs={4}>
                        <Deal product={products?.[2]} />
                    </Col>
                </Row>
            </Container>
        </div>
    ) : null;
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
export default Home;
