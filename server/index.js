require('dotenv').config();
const express = require('express');
const cors = require('cors');
const feedbackRouter = require('./routes/feedback');

const app = express();
const PORT = process.env.PORT ?? 3000;

app.use(cors());
app.use(express.json({ limit: '10kb' }));

// Routes
app.use('/feedback', feedbackRouter);

app.get('/health', (_, res) => res.json({ ok: true, ts: Date.now() }));

app.listen(PORT, () => {
  console.log(`GrindCheck server running on port ${PORT}`);
});
