import './App.css';

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
        <span>{tags[0]}</span>
        <span>{tags[1]}</span>
      </div>
    </article>
  );
}

export default function App() {
  return (
    <main>
      <h1>Project Alpha</h1>
      <PostCard
        author="cedis"
        createdAt="Today"
        title="React props practice"
        content="PostCard receives data and renders it on the screen."
        tags={['React', 'Props']}
        category="Learning"
      />
    </main>
  );
}
