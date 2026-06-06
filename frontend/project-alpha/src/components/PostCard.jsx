import { Link } from 'react-router-dom';

export default function PostCard({
  id,
  author,
  createdAt,
  title,
  content,
  tags,
  category,
  comments,
  onDelete,
  onEdit,
}) {
  const commentLabel = comments.length === 1 ? '1 comment' : `${comments.length} comments`;

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
      <p>{commentLabel}</p>
      <Link to={`/posts/${id}`}>Open</Link>
    </article>
  );
}
