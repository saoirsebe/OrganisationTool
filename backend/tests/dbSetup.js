const db = require('../src/database');

beforeEach(async () => {
    await db.query('TRUNCATE tasks RESTART IDENTITY');
});
afterAll(async () => {
    await db.end();
});

module.exports = db;