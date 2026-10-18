const request = require('supertest');
const app = require('../src/appBackend');
//const { loadModel } = require('../src/services/durationPredictionService');

//beforeAll(async () => {
//   await loadModel();
//}, 60000);

jest.mock('../src/services/durationPredictionService', () => ({
    predictDuration: jest.fn(),
}));
const { predictDuration } = require('../src/services/durationPredictionService');


describe('POST /api/task_duration_prediction', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('rejects a request with missing description', async () => {
        predictDuration.mockResolvedValue(45);
        const response = await request(app)
            .post('/api/task_duration_prediction')
            .send({});

        expect(response.status).toBe(400);
        expect(response.body).toEqual({
            error: 'Invalid task description data',
        });
        expect(predictDuration).not.toHaveBeenCalled();
    });

    it('rejects a request with blank description', async () => {
        predictDuration.mockResolvedValue(45);
        const response = await request(app)
            .post('/api/task_duration_prediction')
            .send({description:'    '});

        expect(response.status).toBe(400);
        expect(response.body).toEqual({
            error: 'Invalid task description data',
        });
        expect(predictDuration).not.toHaveBeenCalled();
    });

    it('predicts time for a task', async () => {
        predictDuration.mockResolvedValue(45);
        const response = await request(app)
            .post('/api/task_duration_prediction')
            .send({ description: 'Write a report'});

        expect(response.status).toBe(200);
        expect(response.body).toEqual({
            predictedMinutes: 45,
        });
        expect(predictDuration).toHaveBeenCalledWith(
            'Write a report'
        );
    });

    it('trims whitespace from description', async()=>{
        predictDuration.mockResolvedValue(45);
        const response = await request(app)
            .post('/api/task_duration_prediction')
            .send({ description: ' Write a report   '});

        expect(response.status).toBe(200);
        expect(response.body).toEqual({
            predictedMinutes: 45,
        });
        expect(predictDuration).toHaveBeenCalledWith(
            'Write a report'
        );
    });

    it('returns 500 if prediction fails', async () => {
        predictDuration.mockRejectedValue(
            new Error('ONNX inference failed')
        );

        const response = await request(app)
            .post('/api/task_duration_prediction')
            .send({ description: 'Write a report' });

        expect(response.status).toBe(500);
        expect(response.body).toEqual({
            error: 'Prediction failed',
        });
    });

});
