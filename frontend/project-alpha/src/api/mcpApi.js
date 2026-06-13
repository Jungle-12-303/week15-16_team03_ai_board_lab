const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export async function checkWeatherFact(postId, token) {
  const response = await fetch(`${apiBaseUrl}/api/posts/${postId}/fact-check/weather`, {
    method: 'POST',
    headers: authHeaders(token),
  });

  if (!response.ok) {
    throw new Error('Failed to check weather facts.');
  }

  return normalizeWeatherFactCheck(await response.json());
}

function authHeaders(token) {
  if (!token) {
    return {};
  }

  return {
    Authorization: `Bearer ${token}`,
  };
}

function normalizeWeatherFactCheck(result) {
  return {
    status: String(result.status ?? ''),
    message: String(result.message ?? ''),
    toolName: String(result.toolName ?? ''),
    location: String(result.location ?? ''),
    source: String(result.source ?? ''),
    observedAt: String(result.observedAt ?? ''),
    externalFact: String(result.externalFact ?? ''),
    claim: String(result.claim ?? ''),
    verdict: String(result.verdict ?? ''),
    comparison: String(result.comparison ?? ''),
    suggestion: String(result.suggestion ?? ''),
    judgement: String(result.judgement ?? ''),
  };
}
