import { apiBaseUrl, authFetch, withCsrf } from '../http/config';

export async function recommendMissedPosts({ limit = 5 }) {
  const response = await authFetch(`${apiBaseUrl}/api/agent/missed-posts`, await withCsrf({
    method: 'POST',
    headers: jsonHeaders(),
    body: JSON.stringify({
      limit,
    }),
  }));

  if (!response.ok) {
    throw new Error('Failed to load missed post recommendations.');
  }

  const agentResponse = await response.json();
  const recommendations = Array.isArray(agentResponse.recommendations)
    ? agentResponse.recommendations
    : [];
  const steps = Array.isArray(agentResponse.steps) ? agentResponse.steps : [];

  return {
    message: String(agentResponse.message ?? ''),
    summary: String(agentResponse.summary ?? ''),
    recommendations: recommendations.map(normalizeRecommendation),
    steps: steps.map(normalizeStep),
  };
}

function jsonHeaders() {
  return {
    'Content-Type': 'application/json',
  };
}

function normalizeRecommendation(recommendation) {
  return {
    postId: Number(recommendation.postId),
    title: String(recommendation.title ?? ''),
    category: String(recommendation.category ?? ''),
    excerpt: String(recommendation.excerpt ?? ''),
    tags: Array.isArray(recommendation.tags) ? recommendation.tags.map(String) : [],
    score: Number(recommendation.score ?? 0),
    reason: String(recommendation.reason ?? ''),
    scoreBreakdown: normalizeScoreBreakdown(recommendation.scoreBreakdown),
  };
}

function normalizeScoreBreakdown(scoreBreakdown) {
  if (scoreBreakdown === null || typeof scoreBreakdown !== 'object') {
    return null;
  }

  return {
    categoryScore: Number(scoreBreakdown.categoryScore ?? 0),
    tagScore: Number(scoreBreakdown.tagScore ?? 0),
    recencyScore: Number(scoreBreakdown.recencyScore ?? 0),
    categoryContribution: Number(scoreBreakdown.categoryContribution ?? 0),
    tagContribution: Number(scoreBreakdown.tagContribution ?? 0),
    recencyContribution: Number(scoreBreakdown.recencyContribution ?? 0),
    matchedTags: Array.isArray(scoreBreakdown.matchedTags)
      ? scoreBreakdown.matchedTags.map(String)
      : [],
  };
}

function normalizeStep(step) {
  return {
    tool: String(step.tool ?? ''),
    status: String(step.status ?? ''),
    observation: String(step.observation ?? ''),
  };
}
