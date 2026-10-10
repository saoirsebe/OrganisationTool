const express = require('express');
const { predictDuration } = require('../services/duration_prediction_service');

const router = express.Router();

// Checks task_description fsent from frontend and calls predict from duration_prediction_service.js
router.post('/', async (req, res) => {
    const {task_description} = req.body;

    if (
        typeof task_description !== 'string' ||
        task_description.trim().length === 0
    ) {
        return res.status(400).json({
            error: 'Invalid prediction data'
        });
    }

    const trim_task_description = task_description.trim();

    try {
        const predictedMinutes = await predictDuration(trim_task_description);
        res.json({ predictedMinutes });
    } catch (err) {
        console.error('Prediction failed:', err);
        res.status(500).json({ error: 'Prediction failed' });
    }
});

module.exports = router;