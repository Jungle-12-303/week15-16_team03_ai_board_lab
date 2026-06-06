import { useState } from 'react';

export default function PostCard({
  author,
  createdAt,
  title,
  content,
  tags,
  category,
  comments,
  onDelete,
  onEdit,
  onAddComment,
  onDeleteComment,
  canComment,
}) {
  const [commentInput, setCommentInput] = useState('');

  function handleCommentSubmit(event) {
    event.preventDefault();

    if (!canComment || commentInput.trim().length === 0) {
      return;
    }

    onAddComment(commentInput);
    setCommentInput('');
  }

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

      <button type="button" onClick={onDelete}>
        Delete
      </button>
      <button type="button" onClick={onEdit}>
        Edit
      </button>

      <section>
        <h3>Comments</h3>

        {comments.length === 0 ? (
          <p>No comments yet.</p>
        ) : (
          comments.map((comment) => (
            <div key={comment.id}>
              <p>
                {comment.author}: {comment.content}
              </p>
              <button type="button" onClick={() => onDeleteComment(comment.id)}>
                Delete comment
              </button>
            </div>
          ))
        )}

        <form onSubmit={handleCommentSubmit}>
          <input
            value={commentInput}
            onChange={(event) => setCommentInput(event.target.value)}
            placeholder="Write a comment"
            disabled={!canComment}
          />
          <button type="submit" disabled={!canComment || commentInput.trim().length === 0}>
            Add comment
          </button>
        </form>
      </section>
    </article>
  );
}
