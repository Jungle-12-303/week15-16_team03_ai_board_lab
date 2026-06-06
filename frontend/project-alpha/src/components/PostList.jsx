import PostCard from './PostCard';

export default function PostList({ posts }) {
  if (posts.length === 0) {
    return <p>No posts yet.</p>;
  }

  return (
    <>
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
    </>
  );
}

