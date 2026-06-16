import PostCard from './PostCard';

export default function PostList({ posts }) {
  if (posts.length === 0) {
    return <p className="empty-state">No posts yet.</p>;
  }

  return (
    <section className="feed-list">
      {posts.map((post) => (
        <PostCard
          key={post.id}
          id={post.id}
          author={post.author}
          createdAt={post.createdAt}
          title={post.title}
          content={post.content}
          tags={post.tags}
          category={post.category}
          comments={post.comments}
        />
      ))}
    </section>
  );
}

