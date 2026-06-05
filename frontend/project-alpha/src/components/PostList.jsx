import PostCard from './PostCard';

export default function PostList({ posts, onDeletePost, onEditPost }) {
  if (posts.length === 0) {
    return <p>No posts yet.</p>;
  }

  return (
    <>
      {posts.map((post) => (
        <PostCard
          key={post.id}
          author={post.author}
          createdAt={post.createdAt}
          title={post.title}
          content={post.content}
          tags={post.tags}
          category={post.category}
          onDelete={() => onDeletePost(post.id)}
          onEdit={() => onEditPost(post.id)}
        />
      ))}
    </>
  );
}
