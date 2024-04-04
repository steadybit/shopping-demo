import 'bootstrap/dist/css/bootstrap.min.css';

import { Container, Navbar } from 'react-bootstrap';
import { Route, HashRouter as Router, Switch } from 'react-router-dom';

import Home from './pages/Home';
import { Logo } from './images';
import React from 'react';
import { Version } from './services/ProductService';

const App: React.FC = () => {
    return (
        <Container fluid>
            <Navbar sticky="top" bg={'dark'} variant="dark" expand="lg">
                <Container>
                    <Navbar.Brand href="#home">
                        <Logo /> Swag Shop
                    </Navbar.Brand>
                </Container>
            </Navbar>
            <Router>
                <Switch>
                    <Route path={'/:version?'}>{({ match }) => <Home version={match?.params.version as Version} />}</Route>
                </Switch>
            </Router>
        </Container>
    );
};

export default App;
