const express = require('express');
const { scheduleDay , scheduleWeek} = require('../services/schedulerService');
const router = express.Router();

// POST /api/schedule/day   body: { userId: userId, date: date }
router.post('/day', async(req, res, next) => {
    try {
        const { userId: userId, date: date} = req.body;
        const result = await scheduleDay(userId, date);
        res.json(result);
    } catch (err) { next(err); }
});

// POST /api/schedule/week   body: { userId: userId, startDate: date }
router.post('/week', async (req, res, next) => {
    try {
        const { userId: userId, startDate: startDate} = req.body;
        const result = await scheduleWeek(userId, startDate);
        res.json(result);
    } catch (err) { next(err); }
});

module.exports = router;