const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
    app.use(
        ['/products', '/checkout'],
        createProxyMiddleware({
            target: 'http://localhost:8080',
            changeOrigin: true,
            xfwd: true
        })
    );
};