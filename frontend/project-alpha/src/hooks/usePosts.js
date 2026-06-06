import { useEffect, useState } from 'react';
import { loadStoredPosts, saveStoredPosts } from '../storage/postStorage';

export default function usePosts(currentUser) {
  const [posts, setPosts] = useState(loadStoredPosts);

  useEffect(() => {
    saveStoredPosts(posts);
  }, [posts]);

  function createPost({ title, content, category, tags }) {
    if (currentUser === null) {
      return null;
    }

    const newPost = {
      id: Date.now(),
      author: currentUser.name,
      createdAt: 'Just now',
      title: title,
      content: content,
      category: category,
      tags: tags,
      comments: [],
    };

    setPosts((currentPosts) => [newPost, ...currentPosts]);
    return newPost;
  }

  function updatePost(postId, nextPostFields) {
    setPosts((currentPosts) =>
      currentPosts.map((post) =>
        post.id === postId
          ? {
              ...post,
              ...nextPostFields,
            }
          : post,
      ),
    );
  }

  function deletePost(postId) {
    setPosts((currentPosts) => currentPosts.filter((post) => post.id !== postId));
  }

  function addComment(postId, commentContent) {
    const trimmedContent = commentContent.trim();

    if (currentUser === null || trimmedContent.length === 0) {
      return;
    }

    setPosts((currentPosts) =>
      currentPosts.map((post) =>
        post.id === postId
          ? {
              ...post,
              comments: [
                ...post.comments,
                {
                  id: Date.now(),
                  author: currentUser.name,
                  content: trimmedContent,
                },
              ],
            }
          : post,
      ),
    );
  }

  function deleteComment(postId, commentId) {
    setPosts((currentPosts) =>
      currentPosts.map((post) =>
        post.id === postId
          ? {
              ...post,
              comments: post.comments.filter((comment) => comment.id !== commentId),
            }
          : post,
      ),
    );
  }

  return {
    posts,
    createPost,
    updatePost,
    deletePost,
    addComment,
    deleteComment,
  };
}
