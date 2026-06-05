export default function PostCard({
  author,
  createdAt,
  title,
  content,
  tags,
  category,
  onDelete,
  onEdit,
}) {
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
    </article>
  );
}
