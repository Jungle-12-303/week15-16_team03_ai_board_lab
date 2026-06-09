const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export async function fetchPosts({ keyword = '', category = 'All', page = 0, size = 100 } = {}) {
  const searchParams = new URLSearchParams({
    keyword,
    category,
    page: String(page),
    size: String(size),
  });
  const response = await fetch(`${apiBaseUrl}/api/posts?${searchParams.toString()}`);

  if (!response.ok) {
    throw new Error('Failed to load posts.');
  }

  const postsResponse = await response.json();
  const posts = Array.isArray(postsResponse) ? postsResponse : postsResponse.posts;

  if (!Array.isArray(posts)) {
    throw new Error('Posts response must be an array.');
  }

  return {
    posts: posts.map(normalizePost),
    page: Number(postsResponse.page ?? 0),
    size: Number(postsResponse.size ?? posts.length),
    totalElements: Number(postsResponse.totalElements ?? posts.length),
    totalPages: Number(postsResponse.totalPages ?? 1),
  };
}

export async function createPost({ title, content, category, tags, token }) {
  const response = await fetch(`${apiBaseUrl}/api/posts`, {
    method: 'POST',
    headers: jsonHeaders(token),
    body: JSON.stringify({
      title,
      content,
      category,
      tags,
    }),
  });

  if (!response.ok) {
    throw new Error('Failed to create post.');
  }

  return normalizePost(await response.json());
}

export async function updatePost(postId, { title, content, category, tags, token }) {
  const response = await fetch(`${apiBaseUrl}/api/posts/${postId}`, {
    method: 'PATCH',
    headers: jsonHeaders(token),
    body: JSON.stringify({
      title,
      content,
      category,
      tags,
    }),
  });

  if (!response.ok) {
    throw new Error('Failed to update post.');
  }

  return normalizePost(await response.json());
}

export async function deletePost(postId, token) {
  const response = await fetch(`${apiBaseUrl}/api/posts/${postId}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  });

  if (!response.ok) {
    throw new Error('Failed to delete post.');
  }
}

export async function addComment(postId, { content, token }) {
  const response = await fetch(`${apiBaseUrl}/api/posts/${postId}/comments`, {
    method: 'POST',
    headers: jsonHeaders(token),
    body: JSON.stringify({
      content,
    }),
  });

  if (!response.ok) {
    throw new Error('Failed to add comment.');
  }

  return normalizeComment(await response.json());
}

export async function deleteComment(postId, commentId, token) {
  const response = await fetch(`${apiBaseUrl}/api/posts/${postId}/comments/${commentId}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  });

  if (!response.ok) {
    throw new Error('Failed to delete comment.');
  }
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

function normalizePost(post) {
  return {
    id: Number(post.id),
    author: String(post.author ?? ''),
    category: String(post.category ?? ''),
    createdAt: String(post.createdAt ?? ''),
    title: String(post.title ?? ''),
    content: String(post.content ?? ''),
    tags: Array.isArray(post.tags) ? post.tags.map(String) : [],
    comments: Array.isArray(post.comments) ? post.comments.map(normalizeComment) : [],
  };
}

function normalizeComment(comment) {
  return {
    id: Number(comment.id),
    author: String(comment.author ?? ''),
    content: String(comment.content ?? ''),
  };
}
