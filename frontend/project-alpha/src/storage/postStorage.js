import { initialPosts } from '../data/seedData';

const postsStorageKey = 'project-alpha-posts';

export function loadStoredPosts() {
  const storedPosts = localStorage.getItem(postsStorageKey);

  if (storedPosts === null) {
    return initialPosts;
  }

  try {
    const parsedPosts = JSON.parse(storedPosts);

    if (!Array.isArray(parsedPosts)) {
      return initialPosts;
    }

    return parsedPosts.filter(
      (post) =>
        typeof post.id === 'number' &&
        typeof post.author === 'string' &&
        typeof post.category === 'string' &&
        typeof post.createdAt === 'string' &&
        typeof post.title === 'string' &&
        typeof post.content === 'string' &&
        Array.isArray(post.tags) &&
        Array.isArray(post.comments),
    );
  } catch {
    return initialPosts;
  }
}

export function saveStoredPosts(posts) {
  localStorage.setItem(postsStorageKey, JSON.stringify(posts));
}
