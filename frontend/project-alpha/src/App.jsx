import { useState } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import './App.css';
import Topbar from './components/Topbar';
import { postsPerPage } from './constants/board';
import useAgentRecommendations from './hooks/useAgentRecommendations';
import useAuth from './hooks/useAuth';
import usePostComposer from './hooks/usePostComposer';
import usePosts from './hooks/usePosts';
import useRagDraft from './hooks/useRagDraft';
import BoardPage from './pages/BoardPage';
import LoginPage from './pages/LoginPage';
import PostDetailPage from './pages/PostDetailPage';
import SignupPage from './pages/SignupPage';

export default function App() {
  const location = useLocation();
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [selectedTag, setSelectedTag] = useState('');
  const { currentUser, isLoadingAuth, login, signUp, logout } = useAuth();
  const composer = usePostComposer();
  const ragDraft = useRagDraft(currentUser);
  const agentRecommendations = useAgentRecommendations(currentUser);
  const {
    posts,
    postPageInfo,
    isLoadingPosts,
    postsError,
    createPost,
    updatePost,
    deletePost,
    addComment,
    deleteComment,
  } = usePosts(currentUser, {
    searchTerm,
    selectedCategory,
    selectedTag,
    currentPage,
    postsPerPage,
  });

  const isLoggedIn = currentUser !== null;
  const canSubmit = isLoggedIn && composer.canSubmit;
  const totalPages = postPageInfo.totalPages;
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const paginatedPosts = posts;
  const filteredPostCount = postPageInfo.totalElements;

  async function handleSubmit(event) {
    event.preventDefault();

    if (!canSubmit || currentUser === null) {
      return;
    }

    const nextTags = composer.getTags();

    if (composer.editingPostId !== null) {
      const updatedPost = await updatePost(composer.editingPostId, {
        title: composer.title,
        content: composer.content,
        category: composer.category,
        tags: nextTags,
      });

      if (updatedPost === null) {
        return;
      }

      composer.clearAfterSave();
      return;
    }

    const createdPost = await createPost({
      title: composer.title,
      content: composer.content,
      category: composer.category,
      tags: nextTags,
    });

    if (createdPost === null) {
      return;
    }

    setCurrentPage(1);
    composer.clearAfterSave();
    ragDraft.resetRagDraft();
  }

  async function handleFindSimilarPosts() {
    if (!canSubmit || currentUser === null) {
      return;
    }

    await ragDraft.loadSimilarPosts(getRagInput());
  }

  async function handleCreateDraftFromSources() {
    if (!canSubmit || currentUser === null) {
      return;
    }

    const draftResult = await ragDraft.generateDraft(getRagInput());

    if (draftResult !== null && draftResult.draft.trim().length > 0) {
      composer.setContent(draftResult.draft);
    }
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

  async function handleLogout() {
    await logout();
    cancelEditPost();
    agentRecommendations.resetAgentRecommendations();
  }

  function cancelEditPost() {
    composer.resetComposer();
    ragDraft.resetRagDraft();
  }

  function startEditPost(postOrId) {
    const postToEdit =
      typeof postOrId === 'object' ? postOrId : posts.find((post) => post.id === postOrId);

    if (!postToEdit) {
      return;
    }

    composer.startEditPost(postToEdit);
    ragDraft.resetRagDraft();
  }

  function getRagInput() {
    return {
      category: composer.category,
      title: composer.title,
      content: composer.content,
      tags: composer.getTags(),
      excludedPostId: composer.editingPostId,
      limit: 5,
    };
  }

  if (isLoadingAuth) {
    return (
      <main className="auth-page">
        <h1>Project Alpha</h1>
        <section className="auth-card">
          <h2>Loading session...</h2>
        </section>
      </main>
    );
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
      categoryCounts={postPageInfo.categoryCounts}
      paginatedPosts={paginatedPosts}
      currentPage={safeCurrentPage}
      totalPages={totalPages}
      filteredPostCount={filteredPostCount}
      agentRecommendations={agentRecommendations}
      searchTerm={searchTerm}
      selectedCategory={selectedCategory}
      selectedTag={selectedTag}
      isComposerOpen={composer.isComposerOpen}
      category={composer.category}
      title={composer.title}
      content={composer.content}
      tagInput={composer.tagInput}
      similarPosts={ragDraft.similarPosts}
      similarPostsError={ragDraft.similarPostsError}
      isLoadingSimilarPosts={ragDraft.isLoadingSimilarPosts}
      hasSearchedSimilarPosts={ragDraft.hasSearchedSimilarPosts}
      draftError={ragDraft.draftError}
      draftMessage={ragDraft.draftMessage}
      isGeneratingDraft={ragDraft.isGeneratingDraft}
      canSubmit={canSubmit}
      isEditing={composer.editingPostId !== null}
      onLogout={handleLogout}
      onSearchChange={changeSearchTerm}
      onSelectCategory={selectCategory}
      onTagChange={changeSelectedTag}
      onResetFilters={resetFilters}
      onOpenComposer={composer.openComposer}
      onPageChange={setCurrentPage}
      onCategoryChange={composer.setCategory}
      onTitleChange={composer.setTitle}
      onContentChange={composer.setContent}
      onTagInputChange={composer.setTagInput}
      onFindSimilarPosts={handleFindSimilarPosts}
      onCreateDraftFromSources={handleCreateDraftFromSources}
      onSubmit={handleSubmit}
      onCloseComposer={cancelEditPost}
    />
  );
}
