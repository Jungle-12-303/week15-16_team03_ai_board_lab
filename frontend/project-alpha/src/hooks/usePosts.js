import { useEffect, useState } from 'react';
import {
  addComment as addServerComment,
  createPost as createServerPost,
  deleteComment as deleteServerComment,
  deletePost as deleteServerPost,
  fetchPosts,
  updatePost as updateServerPost,
} from '../api/postApi';

const defaultPostPageInfo = {
  page: 0,
  size: 100,
  totalElements: 0,
  totalPages: 1,
  categoryCounts: {},
};

export default function usePosts(currentUser, postQuery = {}) {
  const {
    searchTerm = '',
    selectedCategory = 'All',
    selectedTag = '',
    currentPage = 1,
    postsPerPage = 100,
  } = postQuery;
  const [posts, setPosts] = useState([]);
  const [postPageInfo, setPostPageInfo] = useState(defaultPostPageInfo);
  const [isLoadingPosts, setIsLoadingPosts] = useState(true);
  const [postsError, setPostsError] = useState('');

  useEffect(() => {
    let ignore = false;

    async function loadServerPosts() {
      try {
        setIsLoadingPosts(true);

        const serverPostPage = await fetchPosts({
          keyword: searchTerm,
          category: selectedCategory,
          tag: selectedTag,
          page: currentPage - 1,
          size: postsPerPage,
        });

        if (!ignore) {
          setPosts(serverPostPage.posts);
          setPostPageInfo({
            page: serverPostPage.page,
            size: serverPostPage.size,
            totalElements: serverPostPage.totalElements,
            totalPages: Math.max(1, serverPostPage.totalPages),
            categoryCounts: serverPostPage.categoryCounts,
          });
          setPostsError('');
        }
      } catch {
        if (!ignore) {
          setPosts([]);
          setPostPageInfo(defaultPostPageInfo);
          setPostsError('Server posts are unavailable.');
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
  }, [searchTerm, selectedCategory, selectedTag, currentPage, postsPerPage]);

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
    if (currentUser === null) {
      return null;
    }

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
    if (currentUser === null) {
      return false;
    }

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

  async function deleteComment(postId, commentId) {
    if (currentUser === null) {
      return false;
    }

    try {
      await deleteServerComment(postId, commentId);

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
      setPostsError('');
      return true;
    } catch {
      setPostsError('Comment could not be deleted on the server.');
      return false;
    }
  }

  return {
    posts,
    postPageInfo,
    isLoadingPosts,
    postsError,
    createPost,
    updatePost,
    deletePost,
    addComment,
    deleteComment,
  };
}
