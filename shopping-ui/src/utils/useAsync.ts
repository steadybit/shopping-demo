import * as React from 'react';

const useAsync = <S = undefined>(initalState: S, stateProvider: (() => Promise<S>) | Promise<S>, deps?: React.DependencyList):
    [S, { isLoading: boolean; error: Error | undefined }, (v: S) => void] => {
    const [data, setData] = React.useState<S>(initalState);
    const [isLoading, setIsLoading] = React.useState(true);
    const [error, setError] = React.useState(undefined);

    const normStateProvider = typeof stateProvider === 'function' ? stateProvider : () => stateProvider;
    const normDeps = deps || (typeof stateProvider === 'function' ? [] : [stateProvider]);
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
                !canceled && setError(e);
            } finally {
                !canceled && setIsLoading(false);
            }
        })();

        return () => {
            canceled = true;
        }
    }, [memoizedStateProvider]);

    return [data, {isLoading, error}, setData];
};

export default useAsync;
