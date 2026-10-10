const express = require('express');
const cors = require('cors');
const app = express();
const port = process.env.PORT || 3000;

app.use(cors()); // allows frontend to communicate with the API.
app.use(express.json()); // for parsing JSON request bodies.


// Database connection test:
require('dotenv').config({
    path: require('path').join(__dirname, '../.env')});

const { Pool } = require('pg');

console.log('DATABASE_URL loaded:', !!process.env.DATABASE_URL);

const pool = new Pool({
    connectionString: process.env.DATABASE_URL
});

async function testDatabase() {
    try {
        const result = await pool.query('SELECT NOW()');

        console.log('Database connected!');
        console.log(result.rows[0]);
    } catch (error) {
        console.error('Database connection failed:');
        console.error(error);
    }
}

testDatabase();

//

const predictionRoutes = require('./routes/duration_prediction_route');
app.use('/api/task_duration_prediction', predictionRoutes);

const { loadModel } = require('./services/duration_prediction_service');


// 404 handler
app.use((req, res) => {
    res.status(404).json({
        error: 'Route not found'
    });
});

// Error handler
app.use((err, req, res, next) => {
    console.error(err);

    res.status(500).json({
        error: 'Internal server error'
    });
});


// Start the server
loadModel()
    .then(() => app.listen(port, () => console.log(`Server is running on http://localhost:${port}`)))
    .catch(err => {
        console.error('Failed to load model:', err);
        process.exit(1);
    });

