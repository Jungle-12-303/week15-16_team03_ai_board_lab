import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { checkFact } from '../api/ai/mcpApi';
import { fetchPost } from '../api/posts/postApi';
import FactCheckPanel from '../components/ai/FactCheckPanel';

export default function PostDetailPage({
  currentUser,
  onAddComment,
  onDeleteComment,
  onDeletePost,
  onEditPost,
}) {
  const { postId } = useParams();
  const navigate = useNavigate();
  const [post, setPost] = useState(null);
  const [isLoadingPost, setIsLoadingPost] = useState(true);
  const [postError, setPostError] = useState('');
  const [factCheck, setFactCheck] = useState(null);
  const [factCheckError, setFactCheckError] = useState('');
  const [isCheckingFact, setIsCheckingFact] = useState(false);
  const [commentInput, setCommentInput] = useState('');
  const activePostIdRef = useRef(null);

  useEffect(() => {
    let ignore = false;
    activePostIdRef.current = String(postId);

    async function loadPost() {
      try {
        setIsLoadingPost(true);
        setCommentInput('');
        setFactCheck(null);
        setFactCheckError('');
        setIsCheckingFact(false);

        const nextPost = await fetchPost(postId);

        if (!ignore) {
          setPost(nextPost);
          setPostError('');
        }
      } catch {
        if (!ignore) {
          setPost(null);
          setPostError('Post could not be loaded.');
        }
      } finally {
        if (!ignore) {
          setIsLoadingPost(false);
        }
      }
    }

    loadPost();

    return () => {
      ignore = true;
      if (activePostIdRef.current === String(postId)) {
        activePostIdRef.current = null;
      }
    };
  }, [postId]);

  async function reloadPost() {
    try {
      const nextPost = await fetchPost(postId);
      setPost(nextPost);
      setPostError('');
    } catch {
      setPostError('Post could not be refreshed.');
    }
  }

  async function handleCheckFact() {
    if (post === null) {
      return;
    }

    const checkedPostId = String(post.id);

    try {
      setIsCheckingFact(true);
      setFactCheckError('');

      const result = await checkFact(post.id);
      if (activePostIdRef.current === checkedPostId) {
        setFactCheck(result);
      }
    } catch {
      if (activePostIdRef.current === checkedPostId) {
        setFactCheck(null);
        setFactCheckError('MCP fact check could not be completed.');
      }
    } finally {
      if (activePostIdRef.current === checkedPostId) {
        setIsCheckingFact(false);
      }
    }
  }

  if (isLoadingPost) {
    return (
      <section className="empty-state">
        <Link className="plain-link back-link" to="/">
          Back to board
        </Link>
        <h2>Loading post...</h2>
      </section>
    );
  }

  if (!post) {
    return (
      <section className="empty-state">
        <Link className="plain-link back-link" to="/">
          Back to board
        </Link>
        <h2>Post not found</h2>
        {postError.length > 0 && <p className="feed-status error-status">{postError}</p>}
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
    await reloadPost();
  }

  async function handleDeleteComment(commentId) {
    const isDeleted = await onDeleteComment(post.id, commentId);

    if (!isDeleted) {
      return;
    }

    await reloadPost();
  }

  async function handleDeletePost() {
    const isDeleted = await onDeletePost(post.id);

    if (!isDeleted) {
      return;
    }

    navigate('/');
  }

  function handleEditPost() {
    onEditPost(post);
    navigate('/');
  }

  const canManagePost = post.author === currentUser.name;
  const factCheckMetaItems = [
    { label: 'Tool', value: factCheck?.toolName ?? '' },
    { label: 'Repository', value: factCheck?.repository ?? '' },
    { label: 'URL', value: factCheck?.repositoryUrl ?? '' },
    { label: 'Location', value: factCheck?.location ?? '' },
    { label: 'Source', value: factCheck?.source ?? '' },
    { label: 'Observed', value: factCheck?.observedAt ?? '' },
  ];

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

      <FactCheckPanel
        title="MCP fact check"
        result={factCheck}
        error={factCheckError}
        isLoading={isCheckingFact}
        onCheck={handleCheckFact}
        metaItems={factCheckMetaItems}
        externalFactTitle="Fetched MCP data"
      />

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
                    onClick={() => handleDeleteComment(comment.id)}
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
