import React from 'react';
import { Container, Nav, Navbar } from 'react-bootstrap';
import 'bootstrap/dist/css/bootstrap.min.css';
import { AiOutlineShop } from 'react-icons/ai';
import Home from './pages/Home';

const App: React.FC = () => {
    return (
        <Container fluid>
            <Navbar sticky='top' bg={'dark'} variant='dark' expand='lg'>
                <Container>
                    <Navbar.Brand href='#home'>
                        <AiOutlineShop /> Bestsellers
                    </Navbar.Brand>
                    <Navbar.Toggle aria-controls="basic-navbar-nav" />
                    <Navbar.Collapse id='basic-navbar-nav'>
                        <Nav className='mr-auto'>
                            <Nav.Link href='/#'>Home</Nav.Link>
                        </Nav>
                    </Navbar.Collapse>
                </Container>
            </Navbar>
            <Container>
                <Home />
            </Container>
        </Container>
    );
};

export default App;
