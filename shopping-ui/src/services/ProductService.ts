import React, { useCallback } from 'react';

import { Products } from '../../src-gen/ui-api';
import axios from 'axios';
import useAsync from '../utils/useAsync';

axios.defaults.timeout = 3000;

export type Version = 'simple' | 'timeout' | 'exception' | 'parallel' | 'circuitBreaker' | 'retry';

export function useProducts(version: Version) {
    const [timestamp, setTimestamp] = React.useState(new Date());
    const fetchProducts = useCallback((version: Version) => {
        switch (version) {
            case 'timeout':
                return ProductService.timeoutHandling.fetch();
            case 'exception':
                return ProductService.exceptionHandling.fetch();
            case 'parallel':
                return ProductService.parallel.fetch();
            case 'circuitBreaker':
                return ProductService.circuitBreaker.fetch();
            case 'retry':
                return ProductService.retry.fetch();
            default:
                return ProductService.legacy.fetch();
        }
    }, []);
    const [content, { error, isLoading }] = useAsync(
        {
            fashion: [],
            toys: [],
            hotDeals: [],
        },
        () => fetchProducts(version),
        [version, timestamp]
    );

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
    return { timestamp, content, error, isLoading };
}

export const ProductService = {
    legacy: {
        async fetch(): Promise<Products> {
            return (await axios.get('/products')).data;
        },
    },
    parallel: {
        async fetch(): Promise<Products> {
            return (await axios.get('/products/parallel')).data;
        },
    },
    circuitBreaker: {
        async fetch(): Promise<Products> {
            return (await axios.get('/products/circuitbreaker')).data;
        },
    },
    retry: {
        async fetch(): Promise<Products> {
            return (await axios.get('/products/retry')).data;
        },
    },
    exceptionHandling: {
        async fetch(): Promise<Products> {
            return (await axios.get('/products/exception')).data;
        },
    },
    timeoutHandling: {
        async fetch(): Promise<Products> {
            return (await axios.get('/products/timeout')).data;
        },
    },
};
