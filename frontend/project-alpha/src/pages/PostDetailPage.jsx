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
      <section className="empty-state">
        <Link className="plain-link back-link" to="/">
          Back to board
        </Link>
        <h2>Post not found</h2>
      </section>
    );
  }

  async function handleCommentSubmit(event) {
    event.preventDefault();

    if (commentInput.trim().length === 0) {
      return;
    }

    const isAdded = await onAddComment(post.id, commentInput);

    if (!isAdded) {
      return;
    }

    setCommentInput('');
  }

  async function handleDeletePost() {
    const isDeleted = await onDeletePost(post.id);

    if (!isDeleted) {
      return;
    }

    navigate('/');
  }

  function handleEditPost() {
    onEditPost(post.id);
    navigate('/');
  }

  const canManagePost = post.author === currentUser.name;

  return (
    <section className="detail-stack">
      <Link className="plain-link back-link" to="/">
        Back to board
      </Link>

      <article className="box post detail-post">
        <div className="post-top">
          <div>
            <span className="author">{post.author}</span>
            <span className="muted"> - {post.createdAt}</span>
          </div>
          <span className="label">{post.category}</span>
        </div>
        <h2>{post.title}</h2>
        <p>{post.content}</p>

        <div className="tag-list">
          {post.tags.map((tag) => (
            <span className="tag" key={tag}>
              #{tag}
            </span>
          ))}
        </div>

        {canManagePost && (
          <div className="post-actions">
            <button type="button" className="plain-button" onClick={handleEditPost}>
              Edit post
            </button>
            <button type="button" className="danger-button" onClick={handleDeletePost}>
              Delete post
            </button>
          </div>
        )}
      </article>

      <section className="box comments-panel">
        <div className="box-header">
          <span>Comments</span>
          <span className="count">{post.comments.length}</span>
        </div>

        {post.comments.length === 0 ? (
          <p className="empty-comment">No comments yet.</p>
        ) : (
          <div className="comment-list">
            {post.comments.map((comment) => (
              <div className="comment-item" key={comment.id}>
                <div>
                  <span className="author">{comment.author}</span>
                  <p>{comment.content}</p>
                </div>
                {comment.author === currentUser.name && (
                  <button
                    type="button"
                    className="plain-button"
                    onClick={() => onDeleteComment(post.id, comment.id)}
                  >
                    Delete
                  </button>
                )}
              </div>
            ))}
          </div>
        )}

        <form className="comment-form" onSubmit={handleCommentSubmit}>
          <input
            className="input"
            value={commentInput}
            onChange={(event) => setCommentInput(event.target.value)}
            placeholder="Write a comment"
          />
          <button
            type="submit"
            className="primary-button"
            disabled={commentInput.trim().length === 0}
          >
            Add comment
          </button>
        </form>
      </section>
    </section>
  );
}
