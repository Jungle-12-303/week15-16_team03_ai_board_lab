import { useState } from 'react';
import './App.css';

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
  const canSubmit = title.trim().length > 0 && content.trim().length > 0;

  function addPost() {
    if (!canSubmit) {
      return;
    }

    const newPost = {
      id: Date.now(),
      author: 'cedis',
      category: 'Learning',
      createdAt: 'Just now',
      title: title,
      content: content,
      tags: ['React', 'State'],
    };

    setPosts([newPost, ...posts]);
    setTitle('');
    setContent('');
  }

  return (
    <main>
      <h1>Project Alpha</h1>
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

      <button type="button" onClick={addPost} disabled={!canSubmit}>
        Add post
      </button>

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
