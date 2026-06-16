import { useEffect, useRef, useState } from 'react';
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
  const [isContentExpanded, setIsContentExpanded] = useState(false);
  const [canExpandContent, setCanExpandContent] = useState(false);
  const contentRef = useRef(null);
  const commentLabel = comments.length === 1 ? '1 comment' : `${comments.length} comments`;

  useEffect(() => {
    const contentElement = contentRef.current;

    if (!contentElement) {
      return undefined;
    }

    setIsContentExpanded(false);

    function updateCanExpandContent() {
      setCanExpandContent(contentElement.scrollHeight > contentElement.clientHeight + 1);
    }

    const frameId = requestAnimationFrame(updateCanExpandContent);
    window.addEventListener('resize', updateCanExpandContent);

    return () => {
      cancelAnimationFrame(frameId);
      window.removeEventListener('resize', updateCanExpandContent);
    };
  }, [content]);

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
      <p
        className={`post-content${isContentExpanded ? ' expanded' : ''}`}
        ref={contentRef}
      >
        {content}
      </p>
      {canExpandContent && (
        <button
          className="content-toggle"
          type="button"
          onClick={() => setIsContentExpanded((currentValue) => !currentValue)}
        >
          {isContentExpanded ? '접기' : '펼쳐보기'}
        </button>
      )}

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
