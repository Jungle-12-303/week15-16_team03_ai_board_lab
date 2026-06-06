export default function PostForm({
  categories,
  category,
  title,
  content,
  tagInput,
  canSubmit,
  onCategoryChange,
  onTitleChange,
  onContentChange,
  onTagInputChange,
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
          <button type="button" className="assist-button">
            Related posts
          </button>
          <button type="button" className="assist-button">
            Draft from sources
          </button>
          <button type="button" className="assist-button">
            Weather post
          </button>
        </div>

        <button type="submit" className="primary-button" disabled={!canSubmit}>
          {isEditing ? 'Update post' : 'Publish'}
        </button>
      </div>

      {isEditing && (
        <button type="button" className="plain-button" onClick={onCancelEdit}>
          Cancel edit
        </button>
      )}
    </form>
  );
}
