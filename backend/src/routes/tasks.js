const express = require('express');
const router = express.Router();
const taskService = require('../services/taskService');

// POST /api/tasks  -> create a task
router.post('/', async (req, res) => {
    try {
        const { userId, description, date, time, duration, isFixed } = req.body;

        // Validate input before touching the database
        if (!userId || !description || !description.trim()) {
            console.log('Request body:', req.body);
            console.log('userId:', req.body?.userId);
            if (!userId){
                return res.status(400).json({ error: 'userId required' });

            }
            else if(!description){
                return res.status(400).json({ error: 'description required' });

            }
            else{
                return res.status(400).json({ error: 'description trim' });

            }
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


// GET routs to get user's tasks either just by userId or UserId and specific date
router.get('/', async (req, res) => {
    console.log("Query parameters:", req.query);
    try {
        let tasks;

        if(req.query.date){
            // GET /api/tasks?userId=1&date=2026-10-16
            tasks = await taskService.getTasksByUserAndDate(req.query.userId, req.query.date);
        } else{
            // GET /api/tasks?userId=1
            tasks = await taskService.getTasksByUser(req.query.userId);
        }

        res.json(tasks);
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Failed to load tasks' });
    }
});

module.exports = router;