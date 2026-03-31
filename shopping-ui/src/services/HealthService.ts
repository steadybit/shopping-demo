import React, { useCallback } from 'react';
import axios from 'axios';
import useAsync from '../utils/useAsync';

export interface DependencyStatus {
    status: 'UP' | 'DOWN';
    url: string;
    error: string | null;
}

export type DependencyHealth = Record<string, DependencyStatus>;

export function useDependencyHealth() {
    const [timestamp, setTimestamp] = React.useState(new Date());

    const fetchHealth = useCallback(() => {
        return HealthService.fetchDependencies();
    }, []);

    const [content, { error, isLoading }] = useAsync<DependencyHealth>(
        {},
        fetchHealth,
        [timestamp]
    );

    React.useEffect(() => {
        const handle = setInterval(() => {
            if (!isLoading) {
                setTimestamp(new Date());
            }
        }, 3000);
        return () => {
            clearInterval(handle);
        };
    });

    return { content, error, isLoading, timestamp };
}

export const HealthService = {
    async fetchDependencies(): Promise<DependencyHealth> {
        return (await axios.get('/api/health/dependencies')).data;
    },
};
