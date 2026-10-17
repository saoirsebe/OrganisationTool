require('dotenv').config({
    path: require('path').join(__dirname, '../.env')});
const port = process.env.PORT || 3000;
const app = require('./appBackend'); // load app from appBackend


// load prediction onnx model on startup
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

