import PostCard from './PostCard';

export default function PostList({
  posts,
  onDeletePost,
  onEditPost,
  onAddComment,
  onDeleteComment,
}) {
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
          comments={post.comments}
          onDelete={() => onDeletePost(post.id)}
          onEdit={() => onEditPost(post.id)}
          onAddComment={(commentContent) => onAddComment(post.id, commentContent)}
          onDeleteComment={(commentId) => onDeleteComment(post.id, commentId)}
        />
      ))}
    </>
  );
}

