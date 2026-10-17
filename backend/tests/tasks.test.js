const request = require('supertest');
const app = require('../src/appBackend');
const db = require('../src/database');

beforeEach(async () => { await db.query('TRUNCATE tasks RESTART IDENTITY'); });
afterAll(async () => { await db.end(); });

describe('POST /api/tasks', () => {
    it('creates a task', async () => {
        const res = await request(app).post('/api/tasks').send({ description: 'Write report',userId:1 });
        expect(res.status).toBe(201);
        expect(res.body).toMatchObject({ description: 'Write report',userId:1 });
    });

    it('rejects a missing description', async () => {
        const res = await request(app).post('/api/tasks').send({});
        expect(res.status).toBe(400);
    });

    it('rejects a missing userId', async () => {
        const res = await request(app).post('/api/tasks').send({ description: 'Write report'});
        expect(res.status).toBe(400);
    });
});