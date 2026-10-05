const express = require('express');

const router = express.Router();

router.post('/', async (req, res) => {
    const {
        task_name
    } = req.body;

    if (
        typeof task_name !== 'string' ||
        task_name.trim().length === 0
    ) {
        return res.status(400).json({
            error: 'Invalid prediction data'
        });
    }

    const taskName = task_name.trim();

    // Prediction model will go here later

    const prediction = 30;

    res.json({
        prediction
    });
});

module.exports = router;