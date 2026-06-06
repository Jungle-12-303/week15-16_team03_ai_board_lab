import { useState } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import './App.css';
import AuthPanel from './components/AuthPanel';
import PostForm from './components/PostForm';
import PostList from './components/PostList';
import LoginPage from './pages/LoginPage';

const categories = ['Development', 'Learning', 'Project', 'Daily', 'Review', 'Briefing'];
const postsPerPage = 3;

const initialPosts = [
  {
    id: 1,
    author: 'cedis',
    category: 'Learning',
    createdAt: 'Today',
    title: 'React props practice',
    content: 'PostCard receives data and renders it on the screen.',
    tags: ['React', 'Props'],
    comments: [
      {
        id: 1,
        author: 'cedis',
        content: 'Comment state will be added next.',
      },
    ],
  },
];

export default function App() {
  const location = useLocation();
  const [posts, setPosts] = useState(initialPosts);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [tagInput, setTagInput] = useState('');
  const [category, setCategory] = useState('Learning');
  const [editingPostId, setEditingPostId] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [selectedTag, setSelectedTag] = useState('All');
  const [currentUser, setCurrentUser] = useState(null);

  const isLoggedIn = currentUser !== null;
  const canSubmit = isLoggedIn && title.trim().length > 0 && content.trim().length > 0;
  const normalizedSearchTerm = searchTerm.trim().toLowerCase();
  const availableTags = [...new Set(posts.flatMap((post) => post.tags))];
  const filteredPosts = posts.filter((post) => {
    const matchesSearch =
      normalizedSearchTerm.length === 0 ||
      post.title.toLowerCase().includes(normalizedSearchTerm) ||
      post.content.toLowerCase().includes(normalizedSearchTerm) ||
      post.category.toLowerCase().includes(normalizedSearchTerm) ||
      post.tags.some((tag) => tag.toLowerCase().includes(normalizedSearchTerm));

    const matchesCategory =
      selectedCategory === 'All' || post.category === selectedCategory;
    const matchesTag = selectedTag === 'All' || post.tags.includes(selectedTag);

    return matchesSearch && matchesCategory && matchesTag;
  });
  const totalPages = Math.max(1, Math.ceil(filteredPosts.length / postsPerPage));
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const firstPostIndex = (safeCurrentPage - 1) * postsPerPage;
  const paginatedPosts = filteredPosts.slice(firstPostIndex, firstPostIndex + postsPerPage);

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
  }

  function deletePost(postId) {
    setPosts(posts.filter((post) => post.id !== postId));
  }

  function resetFilters() {
    setSearchTerm('');
    setSelectedCategory('All');
    setSelectedTag('All');
    setCurrentPage(1);
  }

  function login(username = 'cedis') {
    setCurrentUser({ name: username });
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
  }

  if (currentUser === null) {
    return (
      <main>
        <h1>Project Alpha</h1>

        <Routes>
          <Route
            path="/login"
            element={<LoginPage currentUser={currentUser} onLogin={login} />}
          />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </main>
    );
  }

  if (location.pathname !== '/') {
    return <Navigate to="/" replace />;
  }

  return (
    <main>
      <h1>Project Alpha</h1>

      <AuthPanel currentUser={currentUser} onLogin={login} onLogout={logout} />

      <PostForm
        categories={categories}
        category={category}
        title={title}
        content={content}
        tagInput={tagInput}
        canSubmit={canSubmit}
        onCategoryChange={setCategory}
        onTitleChange={setTitle}
        onContentChange={setContent}
        onTagInputChange={setTagInput}
        onSubmit={handleSubmit}
        isEditing={editingPostId !== null}
        onCancelEdit={cancelEditPost}
      />

      <label>
        Category filter
        <select
          value={selectedCategory}
          onChange={(event) => {
            setSelectedCategory(event.target.value);
            setCurrentPage(1);
          }}
        >
          <option value="All">All</option>
          {categories.map((categoryName) => (
            <option key={categoryName} value={categoryName}>
              {categoryName}
            </option>
          ))}
        </select>
      </label>

      <label>
        Tag filter
        <select
          value={selectedTag}
          onChange={(event) => {
            setSelectedTag(event.target.value);
            setCurrentPage(1);
          }}
        >
          <option value="All">All</option>
          {availableTags.map((tag) => (
            <option key={tag} value={tag}>
              {tag}
            </option>
          ))}
        </select>
      </label>

      <label>
        Search
        <input
          value={searchTerm}
          onChange={(event) => {
            setSearchTerm(event.target.value);
            setCurrentPage(1);
          }}
          placeholder="Search posts"
        />
      </label>

      <button type="button" onClick={resetFilters}>
        Reset filters
      </button>

      <p>
        Showing page {safeCurrentPage} of {totalPages} ({filteredPosts.length} posts)
      </p>

      <PostList
        posts={paginatedPosts}
        onDeletePost={deletePost}
        onEditPost={startEditPost}
        onAddComment={addComment}
        onDeleteComment={deleteComment}
        canComment={isLoggedIn}
      />

      <div className="pagination">
        <button
          type="button"
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
              onClick={() => setCurrentPage(pageNumber)}
              aria-current={safeCurrentPage === pageNumber ? 'page' : undefined}
            >
              {pageNumber}
            </button>
          );
        })}

        <button
          type="button"
          onClick={() => setCurrentPage(safeCurrentPage + 1)}
          disabled={safeCurrentPage === totalPages}
        >
          Next
        </button>
      </div>
    </main>
  );
}
