const express = require('express');
const Anthropic = require('@anthropic-ai/sdk');

const router = express.Router();
const client = new Anthropic({ apiKey: process.env.ANTHROPIC_API_KEY });

const ERROR_LABELS = {
  knee_cave: 'knees caving inward',
  back_rounding: 'back rounding',
  shallow_squat: 'shallow squat depth',
  flared_elbows: 'flared elbows',
  sagging_hips: 'sagging hips',
  incomplete_range: 'incomplete range of motion',
  hip_sag: 'hip sagging',
  pike: 'hips piking up',
  head_drop: 'head dropping',
  bar_drift: 'bar drifting forward',
  swinging: 'body swinging',
  incomplete_curl: 'incomplete curl',
  elbow_drift: 'elbow drifting forward',
  knee_cave: 'knees caving',
};

function buildPrompt(body) {
  const { exercise, reps, avgScore, mainErrors, worstRep } = body;
  const errorText = mainErrors
    .map((e) => ERROR_LABELS[e] ?? e)
    .join(', ');

  return `You are a gym coach giving form feedback. Reply ONLY in valid JSON with exactly these keys: mistake, fix, vibe. Each value max 20 words.

Exercise: ${exercise}
Reps completed: ${reps}
Average form score: ${avgScore}/100
Main issues: ${errorText || 'none'}
Worst rep: #${(worstRep ?? 0) + 1}

JSON only, no extra text.`;
}

router.post('/', async (req, res) => {
  const { exercise, reps, avgScore, mainErrors = [], worstRep = 0 } = req.body;

  if (!exercise || typeof avgScore !== 'number') {
    return res.status(400).json({ error: 'Invalid payload' });
  }

  if (!process.env.ANTHROPIC_API_KEY) {
    console.error('[/feedback] ANTHROPIC_API_KEY env var not set');
    return res.status(500).json({ error: 'Server misconfigured: ANTHROPIC_API_KEY missing' });
  }

  try {
    const message = await client.messages.create({
      model: process.env.CLAUDE_MODEL || 'claude-sonnet-4-5',
      max_tokens: 256,
      messages: [{ role: 'user', content: buildPrompt(req.body) }],
    });

    const text = message.content[0].text;
    const match = text.match(/\{[\s\S]*\}/);

    if (!match) {
      return res.status(502).json({ error: 'Claude returned non-JSON response', raw: text });
    }

    const feedback = JSON.parse(match[0]);

    // Validate required keys
    if (!feedback.mistake || !feedback.fix || !feedback.vibe) {
      return res.status(502).json({ error: 'Incomplete feedback from Claude' });
    }

    res.json(feedback);
  } catch (err) {
    console.error('[/feedback] Claude error:', err);
    res.status(500).json({
      error: 'AI feedback unavailable',
      details: err.message,
      type: err.constructor?.name,
    });
  }
});

module.exports = router;
