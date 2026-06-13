const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export async function recommendMissedPosts({ limit = 5, token }) {
  const response = await fetch(`${apiBaseUrl}/api/agent/missed-posts`, {
    method: 'POST',
    headers: jsonHeaders(token),
    body: JSON.stringify({
      limit,
    }),
  });

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

function jsonHeaders(token) {
  return {
    'Content-Type': 'application/json',
    ...authHeaders(token),
  };
}

function authHeaders(token) {
  if (!token) {
    return {};
  }

  return {
    Authorization: `Bearer ${token}`,
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
  };
}

function normalizeStep(step) {
  return {
    tool: String(step.tool ?? ''),
    status: String(step.status ?? ''),
    observation: String(step.observation ?? ''),
  };
}
