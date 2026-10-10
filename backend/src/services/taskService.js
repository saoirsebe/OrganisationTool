const pool = require('../database');

async function createTask({ userId, description, date, time, duration, isFixed }) {
    const sql = `
    INSERT INTO tasks (user_id, description, task_date, task_time, duration_minutes, is_fixed)
    VALUES ($1, $2, $3, $4, $5, $6)  --placeholders to protect against SQL injection
    RETURNING id,
              user_id AS "userId",
              description,
              task_date::text AS date,
              task_time::text AS time,
              duration_minutes AS duration,
              is_fixed AS "isFixed"`;

    const values = [userId, description, date, time, duration, isFixed];
    const result = await pool.query(sql, values);
    return result.rows[0]; //Returns row created by db
}

async function getTasksByUser(userId) {
    const sql = `
    SELECT id, user_id AS "userId", description,
           task_date::text AS date, task_time::text AS time,
           duration_minutes AS duration, is_fixed AS "isFixed"
    FROM tasks WHERE user_id = $1
    ORDER BY task_date, task_time`;
    const result = await pool.query(sql, [userId]); //returns all tasks matching user_id as an array
    return result.rows;
}

module.exports = { createTask, getTasksByUser };