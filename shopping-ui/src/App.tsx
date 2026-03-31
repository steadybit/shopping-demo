import 'bootstrap/dist/css/bootstrap.min.css';

import { Container, Nav, Navbar } from 'react-bootstrap';
import { Route, HashRouter as Router, Routes } from 'react-router-dom';

import Home from './pages/Home';
import Overview from './pages/Overview';
import { Logo } from './images';
import React from 'react';

const App: React.FC = () => {
    return (
        <Container fluid>
            <Navbar sticky="top" bg={'dark'} variant="dark" expand="lg">
                <Container>
                    <Navbar.Brand href="#home">
                        <Logo /> Swag Shop
                    </Navbar.Brand>
                    <Nav>
                        <Nav.Link href="#/">Shop</Nav.Link>
                        <Nav.Link href="#/overview">Overview</Nav.Link>
                    </Nav>
                </Container>
            </Navbar>
            <Router>
                <Routes>
                    <Route path={'/overview'} element={<Overview/>}/>
                    <Route path={'/:version?'} element={<Home/>}/>
                </Routes>
            </Router>
        </Container>
    );
};

export default App;
