import * as React from 'react';
import { render, screen } from '@testing-library/react';
import App from './App';

jest.mock('react-icons/ai', () => {
    const icons = {
        __esModule: true
    };

    const handler = {
        get: function(_: any, prop: any) {
            return () => <div className={`mock_${prop}Icon`} />;
        }
    };

    return new Proxy(icons, handler);
});

test('render', () => {
    render(<App />);
    const el = screen.getByText(/Bestsellers/i);
    expect(el).toBeInTheDocument();
});

