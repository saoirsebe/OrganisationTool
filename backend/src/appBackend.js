const express = require('express');
const cors = require('cors');

const app = express();

app.use(cors()); // allows frontend to communicate with the API.
app.use(express.json()); // for parsing JSON request bodies.
app.use(express.static(require('path').join(__dirname, '..', '..', 'frontend', 'home.html'))); // serves the frontend (a container running only Node server won’t deliver HTML files on its own)

// APIs
app.use('/api/tasks', require('./routes/tasks'));

app.use('/api/task_duration_prediction', require('./routes/durationPredictionRoute'));

module.exports = app;
