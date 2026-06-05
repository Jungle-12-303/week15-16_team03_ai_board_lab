import './App.css';

function PostCard() {
  return (
    <article>
      <p>Learning</p>
      <p>cedis - Today</p>
      <h2>React props practice</h2>
      <p>PostCard renders a static post on the screen.</p>

      <div>
        <span>React</span>
        <span>JSX</span>
      </div>
    </article>
  );
}

export default function App() {
  return (
    <main>
      <h1>Project Alpha</h1>
      <PostCard />
    </main>
  );
}
