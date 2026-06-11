const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export async function findSimilarPosts({
  category,
  title,
  content,
  tags,
  excludedPostId,
  limit = 5,
  token,
}) {
  const response = await fetch(`${apiBaseUrl}/api/ai/similar-posts`, {
    method: 'POST',
    headers: jsonHeaders(token),
    body: JSON.stringify({
      category,
      title,
      content,
      tags,
      excludedPostId,
      limit,
    }),
  });

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
  token,
}) {
  const response = await fetch(`${apiBaseUrl}/api/ai/draft`, {
    method: 'POST',
    headers: jsonHeaders(token),
    body: JSON.stringify({
      category,
      title,
      content,
      tags,
      excludedPostId,
      limit,
    }),
  });

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

export async function createExternalFactDraft({
  category,
  title,
  content,
  tags,
  token,
}) {
  const response = await fetch(`${apiBaseUrl}/api/ai/external-facts`, {
    method: 'POST',
    headers: jsonHeaders(token),
    body: JSON.stringify({
      category,
      title,
      content,
      tags,
    }),
  });

  if (!response.ok) {
    throw new Error('Failed to create draft with external facts.');
  }

  const externalFactResponse = await response.json();

  return {
    draft: String(externalFactResponse.draft ?? ''),
    message: String(externalFactResponse.message ?? ''),
    toolName: String(externalFactResponse.toolName ?? ''),
    sources: Array.isArray(externalFactResponse.sources)
      ? externalFactResponse.sources.map(normalizeExternalFactSource)
      : [],
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

function normalizeSimilarPost(post) {
  return {
    postId: Number(post.postId),
    title: String(post.title ?? ''),
    category: String(post.category ?? ''),
    content: String(post.content ?? ''),
    score: Number(post.score ?? 0),
  };
}

function normalizeExternalFactSource(source) {
  return {
    toolName: String(source.toolName ?? ''),
    source: String(source.source ?? ''),
    location: String(source.location ?? ''),
    observedAt: String(source.observedAt ?? ''),
  };
}
