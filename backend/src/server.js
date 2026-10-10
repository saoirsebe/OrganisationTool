require('dotenv').config({
    path: require('path').join(__dirname, '../.env')});
const express = require('express');
const cors = require('cors');
const port = process.env.PORT || 3000;

const app = express();
app.use(cors()); // allows frontend to communicate with the API.
app.use(express.json()); // for parsing JSON request bodies.


// APIs
const taskRoutes = require('./routes/tasks');
app.use('/api/tasks', taskRoutes);

const predictionRoutes = require('./routes/durationPredictionRoute');
app.use('/api/task_duration_prediction', predictionRoutes);

const { loadModel } = require('./services/durationPredictionService');


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

