const db = require('./dbSetup')
const request = require("supertest");
const app = require("../src/appBackend");

describe('POST /api/tasks', () => {
    it('creates a task', async () => {
        const res = await request(app).post('/api/tasks').send({ description: 'Write report', userId:1, priority:2 });
        expect(res.status).toBe(201);
        expect(res.body).toMatchObject({ description: 'Write report', userId:1, priority:2 });
    });

    it('rejects a missing description', async () => {
        const res = await request(app).post('/api/tasks').send({ userId:1, priority:2 });
        expect(res.status).toBe(400);
    });

    it('rejects a missing priority', async () => {
        const res = await request(app).post('/api/tasks').send({ description: 'Write report', userId:1 });
        expect(res.status).toBe(400);
    });

    it('rejects a missing userId', async () => {
        const res = await request(app).post('/api/tasks').send({ description: 'Write report', priority:2 });
        expect(res.status).toBe(400);
    });
});

describe('GET /api/tasks?userId=${userId}', () => {
    it('returns all tasks with userId == 1', async () => {
        const res = await db.query(
            `INSERT INTO tasks (user_id, description, priority)
            VALUES ($1, $2, $3), ( $4, $5, $6), ( $7, $8, $9)`,
            [
                1, 'Write report', 1,
                1, 'Review code', 2,
                2, 'Go to the gym', 3
            ]
        );

        const userId = 1;
        const getRes = await request(app).get(`/api/tasks?userId=${userId}`);

        expect(getRes.status).toBe(200);
        expect(getRes.body).toHaveLength(2);
        expect(getRes.body[0].description).toBe('Write report');
        expect(getRes.body[0].userId).toBe(1);
        expect(getRes.body[0].priority).toBe(1);
        expect(getRes.body[1].description).toBe('Review code');
        expect(getRes.body[1].userId).toBe(1);
        expect(getRes.body[0].priority).toBe(2);

    });


});

describe('Task API workflow', () => {
    it('creates two tasks and retrieves task with corresponding userId', async () => {
        const postRes0 = await request(app).post('/api/tasks').send({ description: 'Write report', userId: 1, priority: 1 });
        expect(postRes0.status).toBe(201);
        const postRes1 = await request(app).post('/api/tasks').send({ description: 'Go to the gym', userId: 2, priority: 3 });
        expect(postRes1.status).toBe(201);

        const userId = 1;
        const getRes = await request(app).get(`/api/tasks?userId=${userId}`);

        expect(getRes.status).toBe(200);
        expect(getRes.body).toHaveLength(1);
        expect(getRes.body[0].description).toBe('Write report');
        expect(getRes.body[0].userId).toBe(1);
        expect(getRes.body[0].priority).toBe(1);
    });
});