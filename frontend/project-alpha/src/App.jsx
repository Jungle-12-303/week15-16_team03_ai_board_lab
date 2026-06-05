import './App.css';

const posts = [
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
  return (
    <main>
      <h1>Project Alpha</h1>

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
