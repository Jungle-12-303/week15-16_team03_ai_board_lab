export default function PostForm({
  categories,
  category,
  title,
  content,
  tagInput,
  similarPosts,
  similarPostsError,
  isLoadingSimilarPosts,
  hasSearchedSimilarPosts,
  draftError,
  draftMessage,
  isGeneratingDraft,
  canSubmit,
  onCategoryChange,
  onTitleChange,
  onContentChange,
  onTagInputChange,
  onFindSimilarPosts,
  onCreateDraftFromSources,
  onSubmit,
  isEditing,
  onCancelEdit,
}) {
  return (
    <form className="composer" onSubmit={onSubmit}>
      <div className="composer-row">
        <input
          className="input"
          value={title}
          onChange={(event) => onTitleChange(event.target.value)}
          placeholder="Enter a title"
        />

        <select
          className="select"
          value={category}
          onChange={(event) => onCategoryChange(event.target.value)}
        >
          {categories.map((categoryName) => (
            <option key={categoryName} value={categoryName}>
              {categoryName}
            </option>
          ))}
        </select>
      </div>

      <textarea
        className="textarea"
        value={content}
        onChange={(event) => onContentChange(event.target.value)}
        placeholder="Enter content"
      />

      <input
        className="input"
        value={tagInput}
        onChange={(event) => onTagInputChange(event.target.value)}
        placeholder="Enter tags separated by commas"
      />

      <div className="composer-actions">
        <div className="assist-actions">
          <button
            type="button"
            className="assist-button"
            onClick={onFindSimilarPosts}
            disabled={!canSubmit || isLoadingSimilarPosts}
          >
            Related posts
          </button>
          <button
            type="button"
            className="assist-button"
            onClick={onCreateDraftFromSources}
            disabled={!canSubmit || isGeneratingDraft}
          >
            {isGeneratingDraft ? 'Drafting...' : 'Draft from sources'}
          </button>
          <button type="button" className="assist-button">
            Weather post
          </button>
        </div>

        <button type="submit" className="primary-button" disabled={!canSubmit}>
          {isEditing ? 'Update post' : 'Publish'}
        </button>
      </div>

      {(isLoadingSimilarPosts ||
        isGeneratingDraft ||
        hasSearchedSimilarPosts ||
        similarPostsError.length > 0 ||
        draftError.length > 0 ||
        draftMessage.length > 0 ||
        similarPosts.length > 0) && (
        <section className="assist-panel" aria-label="Related posts results">
          {isLoadingSimilarPosts && <p className="feed-status">Finding related posts...</p>}
          {isGeneratingDraft && <p className="feed-status">Drafting from related posts...</p>}
          {similarPostsError.length > 0 && (
            <p className="feed-status error-status">{similarPostsError}</p>
          )}
          {draftError.length > 0 && (
            <p className="feed-status error-status">{draftError}</p>
          )}
          {draftMessage.length > 0 && <p className="feed-status">{draftMessage}</p>}
          {hasSearchedSimilarPosts &&
            !isLoadingSimilarPosts &&
            similarPostsError.length === 0 &&
            draftMessage.length === 0 &&
            similarPosts.length === 0 && (
              <p className="feed-status">No related posts found.</p>
            )}
          {similarPosts.length > 0 && (
            <ul className="similar-list">
              {similarPosts.map((post) => (
                <li key={post.postId}>
                  <strong>{post.title}</strong>
                  <span>
                    {post.category} · score {post.score.toFixed(3)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {isEditing && (
        <button type="button" className="plain-button" onClick={onCancelEdit}>
          Cancel edit
        </button>
      )}
    </form>
  );
}
