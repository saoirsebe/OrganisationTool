// use test db if in test mode else real db
require('dotenv').config({
    path: require('path').join(__dirname, '..', process.env.NODE_ENV === 'test' ? '.env.test' : '.env'),
});

const { Pool } = require('pg');

if (process.env.NODE_ENV === 'test' && !/_test$/.test(new URL(process.env.DATABASE_URL).pathname)) {
    throw new Error('Refusing to run tests against a non-test database');
}

const pool = new Pool({
    connectionString: process.env.DATABASE_URL
});

module.exports = pool;