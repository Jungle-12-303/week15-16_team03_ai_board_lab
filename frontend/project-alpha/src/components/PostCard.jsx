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
    <article className="box post">
      <div className="post-top">
        <div>
          <span className="author">{author}</span>
          <span className="muted"> - {createdAt}</span>
        </div>
        <span className="label">{category}</span>
      </div>

      <h2>{title}</h2>
      <p>{content}</p>

      <div className="tag-list">
        {tags.map((tag) => (
          <span className="tag" key={tag}>
            #{tag}
          </span>
        ))}
      </div>

      <div className="post-bottom">
        <span className="muted">{commentLabel}</span>
        <Link className="plain-link" to={`/posts/${id}`}>
          Open
        </Link>
      </div>
    </article>
  );
}
