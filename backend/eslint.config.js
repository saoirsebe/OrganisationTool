const js = require('@eslint/js');
const globals = require('globals');

module.exports = [
    {
        ignores: ['node_modules/', 'coverage/'],
    },
    js.configs.recommended,
    {
        languageOptions: { globals: { ...globals.node, ...globals.jest } },
        rules: { 'no-unused-vars': 'warn', eqeqeq: 'error' },
    },
];