import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { checkWeatherFact } from '../api/mcpApi';
import { fetchPost } from '../api/postApi';
import WeatherFactCheckPanel from '../components/WeatherFactCheckPanel';

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
  const [weatherFactCheck, setWeatherFactCheck] = useState(null);
  const [weatherFactCheckError, setWeatherFactCheckError] = useState('');
  const [isCheckingWeatherFact, setIsCheckingWeatherFact] = useState(false);
  const [commentInput, setCommentInput] = useState('');

  useEffect(() => {
    let ignore = false;
    setWeatherFactCheck(null);
    setWeatherFactCheckError('');

    async function loadPost() {
      try {
        setIsLoadingPost(true);

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

  async function handleCheckWeatherFact() {
    if (post === null) {
      return;
    }

    try {
      setIsCheckingWeatherFact(true);
      setWeatherFactCheckError('');

      const result = await checkWeatherFact(post.id, currentUser.token);
      setWeatherFactCheck(result);
    } catch {
      setWeatherFactCheck(null);
      setWeatherFactCheckError('Weather fact check could not be completed.');
    } finally {
      setIsCheckingWeatherFact(false);
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

      <WeatherFactCheckPanel
        result={weatherFactCheck}
        error={weatherFactCheckError}
        isLoading={isCheckingWeatherFact}
        onCheck={handleCheckWeatherFact}
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
