import * as React from 'react';

const useAsync = <S = undefined>(
    initalState: S,
    stateProvider: (() => Promise<S>) | Promise<S>,
    deps?: React.DependencyList
): [S, { isLoading: boolean; error: Error | undefined }, (v: S) => void] => {
    const [data, setData] = React.useState<S>(initalState);
    const [isLoading, setIsLoading] = React.useState(true);
    const [error, setError] = React.useState<Error | undefined>(undefined);

    const normStateProvider = typeof stateProvider === 'function' ? stateProvider : () => stateProvider;
    const normDeps = deps || (typeof stateProvider === 'function' ? [] : [stateProvider]);
    // eslint-disable-next-line
    const memoizedStateProvider = React.useCallback(normStateProvider, normDeps);

    React.useEffect(() => {
        let canceled = false;

        (async () => {
            setIsLoading(true);
            try {
                const result = await memoizedStateProvider();
                !canceled && setData(result);
                setError(undefined);
            } catch (e) {
                !canceled && setError(e as Error);
            } finally {
                !canceled && setIsLoading(false);
            }
        })();

        return () => {
            canceled = true;
        };
    }, [memoizedStateProvider]);

    return [data, { isLoading, error }, setData];
};

export default useAsync;
