import { useState } from 'react';
import './App.css';

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

function PostCard({ author, createdAt, title, content, tags, category }) {
  return (
    <article>
      <p>{category}</p>
      <p>
        {author} - {createdAt}
      </p>
      <h2>{title}</h2>
      <p>{content}</p>

      <div>
        {tags.map((tag) => (
          <span key={tag}>{tag}</span>
        ))}
      </div>
    </article>
  );
}

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

  return (
    <main>
      <h1>Project Alpha</h1>

      <form onSubmit={handleSubmit}>
        <select value={category} onChange={(event) => setCategory(event.target.value)}>
          {categories.map((categoryName) => (
            <option key={categoryName} value={categoryName}>
              {categoryName}
            </option>
          ))}
        </select>

        <input
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="Enter a title"
        />

        <textarea
          value={content}
          onChange={(event) => setContent(event.target.value)}
          placeholder="Enter content"
        />

        <input
          value={tagInput}
          onChange={(event) => setTagInput(event.target.value)}
          placeholder="Enter tags separated by commas"
        />

        <button type="submit" disabled={!canSubmit}>
          Add post
        </button>
      </form>

      {posts.length === 0 ? (
        <p>No posts yet.</p>
      ) : (
        posts.map((post) => (
          <PostCard
            key={post.id}
            author={post.author}
            createdAt={post.createdAt}
            title={post.title}
            content={post.content}
            tags={post.tags}
            category={post.category}
          />
        ))
      )}
    </main>
  );
}
