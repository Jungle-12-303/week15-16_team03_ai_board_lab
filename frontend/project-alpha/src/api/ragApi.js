import { apiBaseUrl, withCredentials } from './config';

export async function findSimilarPosts({
  category,
  title,
  content,
  tags,
  excludedPostId,
  limit = 5,
}) {
  const response = await fetch(`${apiBaseUrl}/api/ai/similar-posts`, withCredentials({
    method: 'POST',
    headers: jsonHeaders(),
    body: JSON.stringify({
      category,
      title,
      content,
      tags,
      excludedPostId,
      limit,
    }),
  }));

  if (!response.ok) {
    throw new Error('Failed to find similar posts.');
  }

  const similarPostsResponse = await response.json();
  const posts = similarPostsResponse.posts;

  if (!Array.isArray(posts)) {
    throw new Error('Similar posts response must be an array.');
  }

  return posts.map(normalizeSimilarPost);
}

export async function createDraftFromSources({
  category,
  title,
  content,
  tags,
  excludedPostId,
  limit = 5,
}) {
  const response = await fetch(`${apiBaseUrl}/api/ai/draft`, withCredentials({
    method: 'POST',
    headers: jsonHeaders(),
    body: JSON.stringify({
      category,
      title,
      content,
      tags,
      excludedPostId,
      limit,
    }),
  }));

  if (!response.ok) {
    throw new Error('Failed to create draft from sources.');
  }

  const draftResponse = await response.json();
  const sources = Array.isArray(draftResponse.sources) ? draftResponse.sources : [];

  return {
    draft: String(draftResponse.draft ?? ''),
    message: String(draftResponse.message ?? ''),
    sources: sources.map(normalizeSimilarPost),
  };
}

function jsonHeaders() {
  return {
    'Content-Type': 'application/json',
  };
}

function normalizeSimilarPost(post) {
  return {
    postId: Number(post.postId),
    title: String(post.title ?? ''),
    category: String(post.category ?? ''),
    content: String(post.content ?? ''),
    score: Number(post.score ?? 0),
    scoreBreakdown: normalizeScoreBreakdown(post.scoreBreakdown),
  };
}

function normalizeScoreBreakdown(scoreBreakdown) {
  if (scoreBreakdown === null || typeof scoreBreakdown !== 'object') {
    return null;
  }

  return {
    vectorScore: Number(scoreBreakdown.vectorScore ?? 0),
    bm25Score: Number(scoreBreakdown.bm25Score ?? 0),
    fusionScore: Number(scoreBreakdown.fusionScore ?? 0),
    keywordAlignmentScore: Number(scoreBreakdown.keywordAlignmentScore ?? 0),
    chunkScore: Number(scoreBreakdown.chunkScore ?? 0),
    matchedTerms: Array.isArray(scoreBreakdown.matchedTerms)
      ? scoreBreakdown.matchedTerms.map(String)
      : [],
  };
}
