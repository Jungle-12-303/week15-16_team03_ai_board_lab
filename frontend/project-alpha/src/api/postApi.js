import { apiBaseUrl, withCredentials } from './config';

export async function fetchPosts({
  keyword = '',
  category = 'All',
  tag = '',
  page = 0,
  size = 100,
} = {}) {
  const searchParams = new URLSearchParams({
    keyword,
    category,
    tag,
    page: String(page),
    size: String(size),
  });
  const response = await fetch(
    `${apiBaseUrl}/api/posts?${searchParams.toString()}`,
    withCredentials(),
  );

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
    categoryCounts: normalizeCategoryCounts(postsResponse.categoryCounts),
  };
}

export async function fetchPost(postId) {
  const response = await fetch(`${apiBaseUrl}/api/posts/${postId}`, withCredentials());

  if (!response.ok) {
    throw new Error('Failed to load post.');
  }

  return normalizePost(await response.json());
}

export async function createPost({ title, content, category, tags }) {
  const response = await fetch(`${apiBaseUrl}/api/posts`, withCredentials({
    method: 'POST',
    headers: jsonHeaders(),
    body: JSON.stringify({
      title,
      content,
      category,
      tags,
    }),
  }));

  if (!response.ok) {
    throw new Error('Failed to create post.');
  }

  return normalizePost(await response.json());
}

export async function updatePost(postId, { title, content, category, tags }) {
  const response = await fetch(`${apiBaseUrl}/api/posts/${postId}`, withCredentials({
    method: 'PATCH',
    headers: jsonHeaders(),
    body: JSON.stringify({
      title,
      content,
      category,
      tags,
    }),
  }));

  if (!response.ok) {
    throw new Error('Failed to update post.');
  }

  return normalizePost(await response.json());
}

export async function deletePost(postId) {
  const response = await fetch(`${apiBaseUrl}/api/posts/${postId}`, withCredentials({
    method: 'DELETE',
  }));

  if (!response.ok) {
    throw new Error('Failed to delete post.');
  }
}

export async function addComment(postId, { content }) {
  const response = await fetch(`${apiBaseUrl}/api/posts/${postId}/comments`, withCredentials({
    method: 'POST',
    headers: jsonHeaders(),
    body: JSON.stringify({
      content,
    }),
  }));

  if (!response.ok) {
    throw new Error('Failed to add comment.');
  }

  return normalizeComment(await response.json());
}

export async function deleteComment(postId, commentId) {
  const response = await fetch(
    `${apiBaseUrl}/api/posts/${postId}/comments/${commentId}`,
    withCredentials({
      method: 'DELETE',
    }),
  );

  if (!response.ok) {
    throw new Error('Failed to delete comment.');
  }
}

function jsonHeaders() {
  return {
    'Content-Type': 'application/json',
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

function normalizeCategoryCounts(categoryCounts) {
  if (!Array.isArray(categoryCounts)) {
    return {};
  }

  return categoryCounts.reduce((counts, categoryCount) => {
    const category = String(categoryCount.category ?? '');

    if (category.length === 0) {
      return counts;
    }

    return {
      ...counts,
      [category]: Number(categoryCount.count ?? 0),
    };
  }, {});
}

function normalizeComment(comment) {
  return {
    id: Number(comment.id),
    author: String(comment.author ?? ''),
    content: String(comment.content ?? ''),
  };
}
