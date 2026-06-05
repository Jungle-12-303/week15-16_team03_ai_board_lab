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
  {
    id: 2,
    author: 'alpha',
    category: 'Daily',
    createdAt: 'Yesterday',
    title: 'Small daily log',
    content: 'Keeping the app lightweight matters.',
    tags: ['Daily', 'UX'],
  },
];

export default function App() {
  const [posts, setPosts] = useState(initialPosts);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [tagInput, setTagInput] = useState('');
  const [category, setCategory] = useState('Learning');
  const canSubmit = title.trim().length > 0 && content.trim().length > 0;

  function handleSubmit(event) {
    event.preventDefault();

    if (!canSubmit) {
      return;
    }

    const newPost = {
      id: Date.now(),
      author: 'cedis',
      category: category,
      createdAt: 'Just now',
      title: title,
      content: content,
      tags: tagInput
        .split(',')
        .map((tag) => tag.trim())
        .filter((tag) => tag.length > 0),
    };

    setPosts([newPost, ...posts]);
    setTitle('');
    setContent('');
    setTagInput('');
  }

  function deletePost(postId) {
    setPosts(posts.filter((post) => post.id !== postId));
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
      />

      <PostList posts={posts} onDeletePost={deletePost} />
    </main>
  );
}
