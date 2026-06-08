import { useEffect, useState } from 'react';
import {
  addComment as addServerComment,
  createPost as createServerPost,
  deletePost as deleteServerPost,
  fetchPosts,
  updatePost as updateServerPost,
} from '../api/postApi';
import { loadStoredPosts, saveStoredPosts } from '../storage/postStorage';

export default function usePosts(currentUser) {
  const [posts, setPosts] = useState(loadStoredPosts);
  const [isLoadingPosts, setIsLoadingPosts] = useState(true);
  const [postsError, setPostsError] = useState('');

  useEffect(() => {
    let ignore = false;

    async function loadServerPosts() {
      try {
        const serverPosts = await fetchPosts();

        if (!ignore) {
          setPosts(serverPosts);
          setPostsError('');
        }
      } catch {
        if (!ignore) {
          setPostsError('Server posts are unavailable. Local data is shown.');
        }
      } finally {
        if (!ignore) {
          setIsLoadingPosts(false);
        }
      }
    }

    loadServerPosts();

    return () => {
      ignore = true;
    };
  }, []);

  useEffect(() => {
    saveStoredPosts(posts);
  }, [posts]);

  async function createPost({ title, content, category, tags }) {
    if (currentUser === null) {
      return null;
    }

    try {
      const savedPost = await createServerPost({
        title: title,
        content: content,
        category: category,
        tags: tags,
        author: currentUser.name,
      });

      setPosts((currentPosts) => [savedPost, ...currentPosts]);
      setPostsError('');
      return savedPost;
    } catch {
      setPostsError('Post could not be saved to the server.');
      return null;
    }
  }

  async function updatePost(postId, nextPostFields) {
    try {
      const updatedPost = await updateServerPost(postId, nextPostFields);

      setPosts((currentPosts) =>
        currentPosts.map((post) => (post.id === postId ? updatedPost : post)),
      );
      setPostsError('');
      return updatedPost;
    } catch {
      setPostsError('Post could not be updated on the server.');
      return null;
    }
  }

  async function deletePost(postId) {
    try {
      await deleteServerPost(postId);

      setPosts((currentPosts) => currentPosts.filter((post) => post.id !== postId));
      setPostsError('');
      return true;
    } catch {
      setPostsError('Post could not be deleted on the server.');
      return false;
    }
  }

  async function addComment(postId, commentContent) {
    const trimmedContent = commentContent.trim();

    if (currentUser === null || trimmedContent.length === 0) {
      return false;
    }

    try {
      const newComment = await addServerComment(postId, {
        author: currentUser.name,
        content: trimmedContent,
      });

      setPosts((currentPosts) =>
        currentPosts.map((post) =>
          post.id === postId
            ? {
                ...post,
                comments: [...post.comments, newComment],
              }
            : post,
        ),
      );
      setPostsError('');
      return true;
    } catch {
      setPostsError('Comment could not be saved to the server.');
      return false;
    }
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
    isLoadingPosts,
    postsError,
    createPost,
    updatePost,
    deletePost,
    addComment,
    deleteComment,
  };
}
