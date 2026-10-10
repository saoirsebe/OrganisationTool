const express = require('express');
const router = express.Router();
const taskService = require('../services/taskService');

// POST /api/tasks  -> create a task
router.post('/', async (req, res) => {
    try {
        const { userId, description, date, time, duration, isFixed } = req.body;

        // Validate input before touching the database
        if (!userId || !description || !description.trim()) {
            return res.status(400).json({ error: 'userId and description are required' });
        }

        const task = await taskService.createTask({
            userId,
            description: description.trim(),
            date: date || null,
            time: time || null,
            duration: duration || null,
            isFixed: !!isFixed,
        });

        res.status(201).json(task);          // 201 = Created
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Failed to save task' });
    }
});

// GET /api/tasks?userId=1  -> list a user's tasks
router.get('/', async (req, res) => {
    try {
        const tasks = await taskService.getTasksByUser(req.query.userId);
        res.json(tasks);
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Failed to load tasks' });
    }
});

module.exports = router;