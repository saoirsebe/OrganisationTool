const express = require('express');
const cors = require('cors');
const app = express();
const port = process.env.PORT || 3000;

app.use(cors()); // allows frontend to communicate with the API.
app.use(express.json()); // for parsing JSON request bodies.

// Define a route for GET requests
app.get('/api/health', (req, res) => {
    res.json({
        status: 'healthy'
    });
});


const predictionRoutes = require('./routes/task_duration_prediction');
app.use('/api/task_duration_predictions', predictionRoutes);


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
app.listen(port, () => {
    console.log(`Server is running on http://localhost:${port}`);
});