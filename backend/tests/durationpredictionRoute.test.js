const request = require('supertest');
const app = require('../src/appBackend');
const { loadModel } = require('../src/services/durationPredictionService');

beforeAll(async () => {
    await loadModel();
}, 60000);

describe('POST /api/task_duration_prediction', () => {
    it('rejects a request with missing input text', async () => {
        const response = await request(app)
            .post('/api/task_duration_prediction')
            .send({});

        expect(response.status).toBe(400);
        expect(response.body).toHaveProperty('error');
    });

    it('predicts time for a task', async () => {
        const res = await request(app)
            .post('/api/task_duration_prediction')
            .send({ description: 'Write report'});

        expect(res.status).toBe(200);
        expect(res.body).toHaveProperty('predictedMinutes');
    });

});
