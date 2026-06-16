import { apiBaseUrl, withCredentials } from './config';

export async function checkFact(postId) {
  const response = await fetch(`${apiBaseUrl}/api/posts/${postId}/fact-check`, withCredentials({
    method: 'POST',
  }));

  if (!response.ok) {
    throw new Error('Failed to check facts.');
  }

  return normalizeFactCheck(await response.json());
}

export async function checkWeatherFact(postId) {
  const response = await fetch(`${apiBaseUrl}/api/posts/${postId}/fact-check/weather`, withCredentials({
    method: 'POST',
  }));

  if (!response.ok) {
    throw new Error('Failed to check weather facts.');
  }

  return normalizeFactCheck(await response.json());
}

export async function checkGitHubFact(postId) {
  const response = await fetch(`${apiBaseUrl}/api/posts/${postId}/fact-check/github`, withCredentials({
    method: 'POST',
  }));

  if (!response.ok) {
    throw new Error('Failed to check GitHub facts.');
  }

  return normalizeFactCheck(await response.json());
}

function normalizeFactCheck(result) {
  return {
    status: String(result.status ?? ''),
    message: String(result.message ?? ''),
    toolName: String(result.toolName ?? ''),
    location: String(result.location ?? ''),
    repository: String(result.repository ?? ''),
    repositoryUrl: String(result.repositoryUrl ?? ''),
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
