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

      <p>{commentLabel}</p>
      <Link to={`/posts/${id}`}>Open</Link>
    </article>
  );
}
