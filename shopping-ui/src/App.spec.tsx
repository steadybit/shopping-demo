import { jest } from '@jest/globals';
import { render, screen } from '@testing-library/react';

jest.unstable_mockModule('react-icons/ai', () => ({
    __esModule: true,
    AiOutlineLoading: () => <div className="mock_AiOutlineLoadingIcon" />,
}));

jest.unstable_mockModule('axios', () => ({
    __esModule: true,
    default: {
        defaults: {},
        get: async (url: string) => ({
            data: url.startsWith('/products') ? { fashion: [], toys: [], hotDeals: [] } : {},
        }),
        post: async () => ({ data: {} }),
    },
}));

const { default: App } = await import('./App');

const renderAt = (hash: string) => {
    window.location.hash = hash;
    return render(<App />);
};

test('render', async () => {
    renderAt('#/');
    expect(await screen.findByText(/Swag Shop/i)).toBeInTheDocument();
});

test('renders the shop on the index route', async () => {
    renderAt('#/');
    expect(await screen.findByRole('button', { name: /as simple implementation/i })).toBeInTheDocument();
});

test('renders the overview on /overview', async () => {
    renderAt('#/overview');
    expect(await screen.findByText(/Architecture Overview/i)).toBeInTheDocument();
});

test('passes the optional :version param to the shop', async () => {
    renderAt('#/retry');
    expect(await screen.findByRole('button', { name: /with retry/i })).toBeInTheDocument();
});
