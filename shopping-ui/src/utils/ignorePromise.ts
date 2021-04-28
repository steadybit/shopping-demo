const ignorePromiseResult = <T>(promise: Promise<T>): void => {
    (async () => {
        try {
            await promise;
        } catch (err) {
            console.log('Ignoring error from promise', err);
        }
    })();
};

export default ignorePromiseResult;
