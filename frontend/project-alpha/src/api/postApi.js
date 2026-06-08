const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export async function fetchPosts() {
  const response = await fetch(`${apiBaseUrl}/api/posts`);

  if (!response.ok) {
    throw new Error('Failed to load posts.');
  }

  const posts = await response.json();

  if (!Array.isArray(posts)) {
    throw new Error('Posts response must be an array.');
  }

  return posts.map(normalizePost);
}

export async function createPost({ title, content, category, tags, author }) {
  const response = await fetch(`${apiBaseUrl}/api/posts`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      title,
      content,
      category,
      tags,
      author,
    }),
  });

  if (!response.ok) {
    throw new Error('Failed to create post.');
  }

  return normalizePost(await response.json());
}

export async function updatePost(postId, { title, content, category, tags }) {
  const response = await fetch(`${apiBaseUrl}/api/posts/${postId}`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
    },
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

export async function deletePost(postId) {
  const response = await fetch(`${apiBaseUrl}/api/posts/${postId}`, {
    method: 'DELETE',
  });

  if (!response.ok) {
    throw new Error('Failed to delete post.');
  }
}

export async function addComment(postId, { author, content }) {
  const response = await fetch(`${apiBaseUrl}/api/posts/${postId}/comments`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      author,
      content,
    }),
  });

  if (!response.ok) {
    throw new Error('Failed to add comment.');
  }

  return normalizeComment(await response.json());
}

export async function deleteComment(postId, commentId) {
  const response = await fetch(`${apiBaseUrl}/api/posts/${postId}/comments/${commentId}`, {
    method: 'DELETE',
  });

  if (!response.ok) {
    throw new Error('Failed to delete comment.');
  }
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
