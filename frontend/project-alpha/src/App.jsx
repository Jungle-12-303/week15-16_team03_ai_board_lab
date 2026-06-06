import { useEffect, useState } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import './App.css';
import AuthPanel from './components/AuthPanel';
import BoardSidebar from './components/BoardSidebar';
import ComposerModal from './components/ComposerModal';
import PostFilterBar from './components/PostFilterBar';
import PostList from './components/PostList';
import { categories, postsPerPage } from './constants/board';
import LoginPage from './pages/LoginPage';
import PostDetailPage from './pages/PostDetailPage';
import SignupPage from './pages/SignupPage';
import {
  loadStoredCurrentUser,
  loadStoredUsers,
  saveStoredCurrentUser,
  saveStoredUsers,
} from './storage/authStorage';
import { loadStoredPosts, saveStoredPosts } from './storage/postStorage';

export default function App() {
  const location = useLocation();
  const [posts, setPosts] = useState(loadStoredPosts);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [tagInput, setTagInput] = useState('');
  const [category, setCategory] = useState('Learning');
  const [editingPostId, setEditingPostId] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [selectedTag, setSelectedTag] = useState('');
  const [currentUser, setCurrentUser] = useState(loadStoredCurrentUser);
  const [users, setUsers] = useState(loadStoredUsers);
  const [isComposerOpen, setIsComposerOpen] = useState(false);

  const isLoggedIn = currentUser !== null;
  const canSubmit = isLoggedIn && title.trim().length > 0 && content.trim().length > 0;
  const normalizedSearchTerm = searchTerm.trim().toLowerCase();
  const normalizedSelectedTag = selectedTag.trim().toLowerCase();
  const filteredPosts = posts.filter((post) => {
    const matchesSearch =
      normalizedSearchTerm.length === 0 ||
      post.title.toLowerCase().includes(normalizedSearchTerm) ||
      post.content.toLowerCase().includes(normalizedSearchTerm) ||
      post.category.toLowerCase().includes(normalizedSearchTerm) ||
      post.tags.some((tag) => tag.toLowerCase().includes(normalizedSearchTerm));

    const matchesCategory =
      selectedCategory === 'All' || post.category === selectedCategory;
    const matchesTag =
      normalizedSelectedTag.length === 0 ||
      post.tags.some((tag) => tag.toLowerCase().includes(normalizedSelectedTag));

    return matchesSearch && matchesCategory && matchesTag;
  });
  const totalPages = Math.max(1, Math.ceil(filteredPosts.length / postsPerPage));
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const firstPostIndex = (safeCurrentPage - 1) * postsPerPage;
  const paginatedPosts = filteredPosts.slice(firstPostIndex, firstPostIndex + postsPerPage);

  useEffect(() => {
    saveStoredUsers(users);
  }, [users]);

  useEffect(() => {
    saveStoredPosts(posts);
  }, [posts]);

  useEffect(() => {
    saveStoredCurrentUser(currentUser);
  }, [currentUser]);

  function handleSubmit(event) {
    event.preventDefault();

    if (!canSubmit || currentUser === null) {
      return;
    }

    const nextTags = tagInput
      .split(',')
      .map((tag) => tag.trim())
      .filter((tag) => tag.length > 0);

    if (editingPostId !== null) {
      setPosts(
        posts.map((post) =>
          post.id === editingPostId
            ? {
                ...post,
                title: title,
                content: content,
                category: category,
                tags: nextTags,
              }
            : post,
        ),
      );

      setEditingPostId(null);
      setTitle('');
      setContent('');
      setTagInput('');
      setIsComposerOpen(false);
      return;
    }

    const newPost = {
      id: Date.now(),
      author: currentUser.name,
      createdAt: 'Just now',
      title: title,
      content: content,
      category: category,
      tags: nextTags,
      comments: [],
    };

    setPosts([newPost, ...posts]);
    setCurrentPage(1);
    setTitle('');
    setContent('');
    setTagInput('');
    setIsComposerOpen(false);
  }

  function deletePost(postId) {
    setPosts(posts.filter((post) => post.id !== postId));
  }

  function resetFilters() {
    setSearchTerm('');
    setSelectedCategory('All');
    setSelectedTag('');
    setCurrentPage(1);
  }

  function selectCategory(nextCategory) {
    setSelectedCategory(nextCategory);
    setCurrentPage(1);
  }

  function changeSelectedTag(nextTag) {
    setSelectedTag(nextTag);
    setCurrentPage(1);
  }

  function login(username, password) {
    const trimmedUsername = username.trim();
    const foundUser = users.find(
      (user) => user.username === trimmedUsername && user.password === password,
    );

    if (!foundUser) {
      return false;
    }

    setCurrentUser({ name: foundUser.username });
    return true;
  }

  function signUp(username, password) {
    const trimmedUsername = username.trim();
    const isUsernameTaken = users.some((user) => user.username === trimmedUsername);

    if (isUsernameTaken) {
      return false;
    }

    setUsers([
      ...users,
      {
        username: trimmedUsername,
        password: password,
      },
    ]);

    return true;
  }

  function logout() {
    setCurrentUser(null);
    cancelEditPost();
  }

  function addComment(postId, commentContent) {
    const trimmedContent = commentContent.trim();

    if (currentUser === null || trimmedContent.length === 0) {
      return;
    }

    setPosts(
      posts.map((post) =>
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
    setPosts(
      posts.map((post) =>
        post.id === postId
          ? {
              ...post,
              comments: post.comments.filter((comment) => comment.id !== commentId),
            }
          : post,
      ),
    );
  }

  function cancelEditPost() {
    setEditingPostId(null);
    setTitle('');
    setContent('');
    setTagInput('');
    setCategory('Learning');
    setIsComposerOpen(false);
  }

  function startEditPost(postId) {
    const postToEdit = posts.find((post) => post.id === postId);

    if (!postToEdit) {
      return;
    }

    setEditingPostId(postToEdit.id);
    setTitle(postToEdit.title);
    setContent(postToEdit.content);
    setCategory(postToEdit.category);
    setTagInput(postToEdit.tags.join(', '));
    setIsComposerOpen(true);
  }

  if (currentUser === null) {
    return (
      <main className="auth-page">
        <h1>Project Alpha</h1>

        <Routes>
          <Route
            path="/login"
            element={<LoginPage currentUser={currentUser} onLogin={login} />}
          />
          <Route
            path="/signup"
            element={<SignupPage currentUser={currentUser} onSignUp={signUp} />}
          />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </main>
    );
  }

  if (location.pathname.startsWith('/posts/')) {
    return (
      <>
        <header className="topbar">
          <div className="topbar-inner">
            <div className="brand">Project Alpha</div>
            <div className="topbar-spacer" />
            <AuthPanel currentUser={currentUser} onLogout={logout} />
          </div>
        </header>

        <main className="detail-shell">
          <Routes>
            <Route
              path="/posts/:postId"
              element={
                <PostDetailPage
                  posts={posts}
                  currentUser={currentUser}
                  onAddComment={addComment}
                  onDeleteComment={deleteComment}
                  onDeletePost={deletePost}
                  onEditPost={startEditPost}
                />
              }
            />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
      </>
    );
  }

  if (location.pathname !== '/') {
    return <Navigate to="/" replace />;
  }

  return (
    <>
      <header className="topbar">
        <div className="topbar-inner">
          <div className="brand">Project Alpha</div>
          <input
            className="search"
            value={searchTerm}
            onChange={(event) => {
              setSearchTerm(event.target.value);
              setCurrentPage(1);
            }}
            placeholder="Search posts, tags, comments"
          />
          <AuthPanel currentUser={currentUser} onLogout={logout} />
        </div>
      </header>

      <main className="layout">
        <BoardSidebar
          posts={posts}
          selectedCategory={selectedCategory}
          onSelectCategory={selectCategory}
        />

        <section className="main-content">
          <PostFilterBar
            selectedCategory={selectedCategory}
            selectedTag={selectedTag}
            onSelectCategory={selectCategory}
            onTagChange={changeSelectedTag}
            onResetFilters={resetFilters}
            onOpenComposer={() => setIsComposerOpen(true)}
          />

          <p className="feed-status">
            Page {safeCurrentPage} of {totalPages} - {filteredPosts.length} posts
          </p>

          <PostList posts={paginatedPosts} />

          <div className="pagination">
            <button
              type="button"
              className="plain-button"
              onClick={() => setCurrentPage(safeCurrentPage - 1)}
              disabled={safeCurrentPage === 1}
            >
              Previous
            </button>

            {Array.from({ length: totalPages }, (_, index) => {
              const pageNumber = index + 1;

              return (
                <button
                  key={pageNumber}
                  type="button"
                  className="plain-button"
                  onClick={() => setCurrentPage(pageNumber)}
                  aria-current={safeCurrentPage === pageNumber ? 'page' : undefined}
                >
                  {pageNumber}
                </button>
              );
            })}

            <button
              type="button"
              className="plain-button"
              onClick={() => setCurrentPage(safeCurrentPage + 1)}
              disabled={safeCurrentPage === totalPages}
            >
              Next
            </button>
          </div>
        </section>
      </main>

      {isComposerOpen && (
        <ComposerModal
          categories={categories}
          category={category}
          title={title}
          content={content}
          tagInput={tagInput}
          canSubmit={canSubmit}
          isEditing={editingPostId !== null}
          onCategoryChange={setCategory}
          onTitleChange={setTitle}
          onContentChange={setContent}
          onTagInputChange={setTagInput}
          onSubmit={handleSubmit}
          onClose={cancelEditPost}
        />
      )}
    </>
  );
}
