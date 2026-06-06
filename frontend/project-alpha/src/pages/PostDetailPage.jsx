import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

export default function PostDetailPage({
  posts,
  currentUser,
  onAddComment,
  onDeleteComment,
  onDeletePost,
  onEditPost,
}) {
  const { postId } = useParams();
  const navigate = useNavigate();
  const [commentInput, setCommentInput] = useState('');
  const post = posts.find((item) => item.id === Number(postId));

  if (!post) {
    return (
      <section>
        <Link to="/">Back to board</Link>
        <h2>Post not found</h2>
      </section>
    );
  }

  function handleCommentSubmit(event) {
    event.preventDefault();

    if (commentInput.trim().length === 0) {
      return;
    }

    onAddComment(post.id, commentInput);
    setCommentInput('');
  }

  function handleDeletePost() {
    onDeletePost(post.id);
    navigate('/');
  }

  function handleEditPost() {
    onEditPost(post.id);
    navigate('/');
  }

  const canManagePost = post.author === currentUser.name;

  return (
    <section>
      <Link to="/">Back to board</Link>

      <article>
        <p>{post.category}</p>
        <p>
          {post.author} - {post.createdAt}
        </p>
        <h2>{post.title}</h2>
        <p>{post.content}</p>

        <div>
          {post.tags.map((tag) => (
            <span key={tag}>{tag}</span>
          ))}
        </div>

        {canManagePost && (
          <div>
            <button type="button" onClick={handleEditPost}>
              Edit post
            </button>
            <button type="button" onClick={handleDeletePost}>
              Delete post
            </button>
          </div>
        )}
      </article>

      <section>
        <h3>Comments</h3>

        {post.comments.length === 0 ? (
          <p>No comments yet.</p>
        ) : (
          post.comments.map((comment) => (
            <div key={comment.id}>
              <p>
                {comment.author}: {comment.content}
              </p>
              {comment.author === currentUser.name && (
                <button type="button" onClick={() => onDeleteComment(post.id, comment.id)}>
                  Delete comment
                </button>
              )}
            </div>
          ))
        )}

        <form onSubmit={handleCommentSubmit}>
          <input
            value={commentInput}
            onChange={(event) => setCommentInput(event.target.value)}
            placeholder="Write a comment"
          />
          <button type="submit" disabled={commentInput.trim().length === 0}>
            Add comment
          </button>
        </form>
      </section>
    </section>
  );
}
