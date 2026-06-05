import { useState } from 'react';
import './App.css';
import PostForm from './components/PostForm';
import PostList from './components/PostList';

const categories = ['Development', 'Learning', 'Project', 'Daily', 'Review', 'Briefing'];

const initialPosts = [
  {
    id: 1,
    author: 'cedis',
    category: 'Learning',
    createdAt: 'Today',
    title: 'React props practice',
    content: 'PostCard receives data and renders it on the screen.',
    tags: ['React', 'Props'],
  },
];

export default function App() {
  const [posts, setPosts] = useState(initialPosts);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [tagInput, setTagInput] = useState('');
  const [category, setCategory] = useState('Learning');
  const [editingPostId, setEditingPostId] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');

  const canSubmit = title.trim().length > 0 && content.trim().length > 0;
  const normalizedSearchTerm = searchTerm.trim().toLowerCase();
  const filteredPosts =
    normalizedSearchTerm.length === 0
      ? posts
      : posts.filter((post) => {
          return (
            post.title.toLowerCase().includes(normalizedSearchTerm) ||
            post.content.toLowerCase().includes(normalizedSearchTerm) ||
            post.category.toLowerCase().includes(normalizedSearchTerm) ||
            post.tags.some((tag) => tag.toLowerCase().includes(normalizedSearchTerm))
          );
        });

  function handleSubmit(event) {
    event.preventDefault();

    if (!canSubmit) {
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
      author: 'cedis',
      createdAt: 'Just now',
      title: title,
      content: content,
      category: category,
      tags: nextTags,
    };

    setPosts([newPost, ...posts]);
    setTitle('');
    setContent('');
    setTagInput('');
  }

  function deletePost(postId) {
    setPosts(posts.filter((post) => post.id !== postId));
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

  return (
    <main>
      <h1>Project Alpha</h1>

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
        Search
        <input
          value={searchTerm}
          onChange={(event) => setSearchTerm(event.target.value)}
          placeholder="Search posts"
        />
      </label>

      <PostList
        posts={filteredPosts}
        onDeletePost={deletePost}
        onEditPost={startEditPost}
      />
    </main>
  );
}
