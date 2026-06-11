import { useState } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import './App.css';
import {
  createDraftFromSources,
  createExternalFactDraft,
  findSimilarPosts,
} from './api/aiApi';
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
  const [similarPosts, setSimilarPosts] = useState([]);
  const [similarPostsError, setSimilarPostsError] = useState('');
  const [isLoadingSimilarPosts, setIsLoadingSimilarPosts] = useState(false);
  const [hasSearchedSimilarPosts, setHasSearchedSimilarPosts] = useState(false);
  const [draftError, setDraftError] = useState('');
  const [draftMessage, setDraftMessage] = useState('');
  const [isGeneratingDraft, setIsGeneratingDraft] = useState(false);
  const [externalFactError, setExternalFactError] = useState('');
  const [externalFactMessage, setExternalFactMessage] = useState('');
  const [isLoadingExternalFacts, setIsLoadingExternalFacts] = useState(false);
  const { currentUser, login, signUp, logout } = useAuth();
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
    currentPage,
    postsPerPage,
  });

  const isLoggedIn = currentUser !== null;
  const canSubmit = isLoggedIn && title.trim().length > 0 && content.trim().length > 0;
  const normalizedSelectedTag = selectedTag.trim().toLowerCase();
  const filteredPosts = posts.filter((post) => {
    const matchesTag =
      normalizedSelectedTag.length === 0 ||
      post.tags.some((tag) => tag.toLowerCase().includes(normalizedSelectedTag));

    return matchesTag;
  });
  const totalPages = postPageInfo.totalPages;
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const paginatedPosts = filteredPosts;
  const filteredPostCount =
    normalizedSelectedTag.length === 0 ? postPageInfo.totalElements : filteredPosts.length;

  async function handleSubmit(event) {
    event.preventDefault();

    if (!canSubmit || currentUser === null) {
      return;
    }

    const nextTags = parseTagInput(tagInput);

    if (editingPostId !== null) {
      const updatedPost = await updatePost(editingPostId, {
        title: title,
        content: content,
        category: category,
        tags: nextTags,
      });

      if (updatedPost === null) {
        return;
      }

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
    resetSimilarPosts();
  }

  async function handleFindSimilarPosts() {
    if (!canSubmit || currentUser === null) {
      return;
    }

    try {
      setIsLoadingSimilarPosts(true);
      setSimilarPostsError('');

      const nextSimilarPosts = await findSimilarPosts({
        category: category,
        title: title,
        content: content,
        tags: parseTagInput(tagInput),
        excludedPostId: editingPostId,
        limit: 5,
        token: currentUser.token,
      });

      setSimilarPosts(nextSimilarPosts);
      setHasSearchedSimilarPosts(true);
    } catch {
      setSimilarPosts([]);
      setSimilarPostsError('Similar posts could not be loaded.');
      setHasSearchedSimilarPosts(true);
    } finally {
      setIsLoadingSimilarPosts(false);
    }
  }

  async function handleCreateDraftFromSources() {
    if (!canSubmit || currentUser === null) {
      return;
    }

    try {
      setIsGeneratingDraft(true);
      setDraftError('');
      setDraftMessage('');
      setSimilarPostsError('');

      const draftResult = await createDraftFromSources({
        category: category,
        title: title,
        content: content,
        tags: parseTagInput(tagInput),
        excludedPostId: editingPostId,
        limit: 5,
        token: currentUser.token,
      });

      if (draftResult.draft.trim().length > 0) {
        setContent(draftResult.draft);
      }

      setSimilarPosts(draftResult.sources);
      setDraftMessage(draftResult.message);
      setHasSearchedSimilarPosts(true);
    } catch {
      setDraftMessage('');
      setDraftError('Draft could not be generated from related posts.');
    } finally {
      setIsGeneratingDraft(false);
    }
  }

  async function handleCreateExternalFactDraft() {
    if (!canSubmit || currentUser === null) {
      return;
    }

    try {
      setIsLoadingExternalFacts(true);
      setExternalFactError('');
      setExternalFactMessage('');

      const draftResult = await createExternalFactDraft({
        category: category,
        title: title,
        content: content,
        tags: parseTagInput(tagInput),
        token: currentUser.token,
      });

      if (draftResult.draft.trim().length > 0) {
        setContent(draftResult.draft);
      }

      setExternalFactMessage(draftResult.message);
    } catch {
      setExternalFactMessage('');
      setExternalFactError('External facts could not be loaded.');
    } finally {
      setIsLoadingExternalFacts(false);
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
    resetSimilarPosts();
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
    resetSimilarPosts();
  }

  function resetSimilarPosts() {
    setSimilarPosts([]);
    setSimilarPostsError('');
    setIsLoadingSimilarPosts(false);
    setHasSearchedSimilarPosts(false);
    setDraftError('');
    setDraftMessage('');
    setIsGeneratingDraft(false);
    setExternalFactError('');
    setExternalFactMessage('');
    setIsLoadingExternalFacts(false);
  }

  function parseTagInput(input) {
    return input
      .split(',')
      .map((tag) => tag.trim())
      .filter((tag) => tag.length > 0);
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
      categoryCounts={postPageInfo.categoryCounts}
      paginatedPosts={paginatedPosts}
      currentPage={safeCurrentPage}
      totalPages={totalPages}
      filteredPostCount={filteredPostCount}
      searchTerm={searchTerm}
      selectedCategory={selectedCategory}
      selectedTag={selectedTag}
      isComposerOpen={isComposerOpen}
      category={category}
      title={title}
      content={content}
      tagInput={tagInput}
      similarPosts={similarPosts}
      similarPostsError={similarPostsError}
      isLoadingSimilarPosts={isLoadingSimilarPosts}
      hasSearchedSimilarPosts={hasSearchedSimilarPosts}
      draftError={draftError}
      draftMessage={draftMessage}
      isGeneratingDraft={isGeneratingDraft}
      externalFactError={externalFactError}
      externalFactMessage={externalFactMessage}
      isLoadingExternalFacts={isLoadingExternalFacts}
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
      onFindSimilarPosts={handleFindSimilarPosts}
      onCreateDraftFromSources={handleCreateDraftFromSources}
      onCreateExternalFactDraft={handleCreateExternalFactDraft}
      onSubmit={handleSubmit}
      onCloseComposer={cancelEditPost}
    />
  );
}
