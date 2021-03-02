import {mount} from 'enzyme';
import * as React from 'react';
import App from './App';

jest.mock('react-icons/all', () => ({
    'AiOutlineShop': 'AiOutlineShop',
    'AiOutlineLoading': 'AiOutlineLoading',
}));

describe('App', () => {

    it('should initially render', () => {
        expect(mount(<App/>)).toHaveLength(1);
    });
});
