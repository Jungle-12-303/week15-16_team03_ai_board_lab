import { useState } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import './App.css';
import Topbar from './components/Topbar';
import { postsPerPage } from './constants/board';
import useAuth from './hooks/useAuth';
import usePosts from './hooks/usePosts';
import BoardPage from './pages/BoardPage';
import LoginPage from './pages/LoginPage';
import PostDetailPage from './pages/PostDetailPage';
import SignupPage from './pages/SignupPage';

export default function App() {
  const location = useLocation();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [tagInput, setTagInput] = useState('');
  const [category, setCategory] = useState('Learning');
  const [editingPostId, setEditingPostId] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [selectedTag, setSelectedTag] = useState('');
  const [isComposerOpen, setIsComposerOpen] = useState(false);
  const { currentUser, login, signUp, logout } = useAuth();
  const {
    posts,
    isLoadingPosts,
    postsError,
    createPost,
    updatePost,
    deletePost,
    addComment,
    deleteComment,
  } = usePosts(currentUser);

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

  async function handleSubmit(event) {
    event.preventDefault();

    if (!canSubmit || currentUser === null) {
      return;
    }

    const nextTags = tagInput
      .split(',')
      .map((tag) => tag.trim())
      .filter((tag) => tag.length > 0);

    if (editingPostId !== null) {
      updatePost(editingPostId, {
        title: title,
        content: content,
        category: category,
        tags: nextTags,
      });

      setEditingPostId(null);
      setTitle('');
      setContent('');
      setTagInput('');
      setIsComposerOpen(false);
      return;
    }

    const createdPost = await createPost({
      title: title,
      content: content,
      category: category,
      tags: nextTags,
    });

    if (createdPost === null) {
      return;
    }

    setCurrentPage(1);
    setTitle('');
    setContent('');
    setTagInput('');
    setIsComposerOpen(false);
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

  function changeSearchTerm(nextSearchTerm) {
    setSearchTerm(nextSearchTerm);
    setCurrentPage(1);
  }

  function handleLogout() {
    logout();
    cancelEditPost();
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
        <Topbar currentUser={currentUser} onLogout={handleLogout} />

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
    <BoardPage
      currentUser={currentUser}
      posts={posts}
      isLoadingPosts={isLoadingPosts}
      postsError={postsError}
      paginatedPosts={paginatedPosts}
      currentPage={safeCurrentPage}
      totalPages={totalPages}
      filteredPostCount={filteredPosts.length}
      searchTerm={searchTerm}
      selectedCategory={selectedCategory}
      selectedTag={selectedTag}
      isComposerOpen={isComposerOpen}
      category={category}
      title={title}
      content={content}
      tagInput={tagInput}
      canSubmit={canSubmit}
      isEditing={editingPostId !== null}
      onLogout={handleLogout}
      onSearchChange={changeSearchTerm}
      onSelectCategory={selectCategory}
      onTagChange={changeSelectedTag}
      onResetFilters={resetFilters}
      onOpenComposer={() => setIsComposerOpen(true)}
      onPageChange={setCurrentPage}
      onCategoryChange={setCategory}
      onTitleChange={setTitle}
      onContentChange={setContent}
      onTagInputChange={setTagInput}
      onSubmit={handleSubmit}
      onCloseComposer={cancelEditPost}
    />
  );
}
