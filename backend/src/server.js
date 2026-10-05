const express = require('express');
const cors = require('cors');
const app = express();
const port = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// Define a route for GET requests
app.get('/api/health', (req, res) => {
    res.json({
        status: 'healthy'
    });
});

// Define a route for POST requests

// Define a route for PUT requests

// Define a route for DELETE requests


// Start the server
app.listen(port, () => {
    console.log(`Server is running on http://localhost:${port}`);
});