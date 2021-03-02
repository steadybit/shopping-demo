import React from 'react';
import { Button, Container, Form, FormControl, Nav, Navbar, NavDropdown } from 'react-bootstrap';
import 'bootstrap/dist/css/bootstrap.min.css';
import { AiOutlineShop } from 'react-icons/all';
import Home from './pages/Home';

const App: React.FC = () => {
    return (
        <Container fluid>
            <Navbar sticky='top' bg={'dark'} variant='dark' expand='lg'>
                <Navbar.Brand href='#home'><AiOutlineShop /> Bestsellers</Navbar.Brand>
                <Navbar.Collapse id='basic-navbar-nav'>
                    <Nav className='mr-auto'>
                        <Nav.Link href='/#'>Home</Nav.Link>
                        <NavDropdown title='Fashion' id='basic-nav-dropdown'>
                            <NavDropdown.Item href='#action/3.1'>Women</NavDropdown.Item>
                            <NavDropdown.Item href='#action/3.2'>Men</NavDropdown.Item>
                            <NavDropdown.Item href='#action/3.3'>Kids</NavDropdown.Item>
                        </NavDropdown>
                        <NavDropdown title='Toys' id='basic-nav-dropdown'>
                            <NavDropdown.Item href='#action/3.1'>Plush Toys</NavDropdown.Item>
                            <NavDropdown.Item href='#action/3.2'>Cars</NavDropdown.Item>
                            <NavDropdown.Item href='#action/3.3'>Drones</NavDropdown.Item>
                        </NavDropdown>
                    </Nav>
                    <Form inline>
                        <FormControl type='text' placeholder='Search' className='mr-sm-2' />
                        <Button variant='outline-success'>Search</Button>
                    </Form>
                </Navbar.Collapse>
            </Navbar>
            <Container>
                <Home />
            </Container>
        </Container>
    );
};

export default App;
