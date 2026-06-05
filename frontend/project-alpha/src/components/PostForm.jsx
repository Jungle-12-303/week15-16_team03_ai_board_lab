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
}) {
  return (
    <form onSubmit={onSubmit}>
      <select
        value={category}
        onChange={(event) => onCategoryChange(event.target.value)}
      >
        {categories.map((categoryName) => (
          <option key={categoryName} value={categoryName}>
            {categoryName}
          </option>
        ))}
      </select>

      <input
        value={title}
        onChange={(event) => onTitleChange(event.target.value)}
        placeholder="Enter a title"
      />

      <textarea
        value={content}
        onChange={(event) => onContentChange(event.target.value)}
        placeholder="Enter content"
      />

      <input
        value={tagInput}
        onChange={(event) => onTagInputChange(event.target.value)}
        placeholder="Enter tags separated by commas"
      />

      <button type="submit" disabled={!canSubmit}>
        {isEditing ? 'Update post' : 'Add post'}
      </button>
    </form>
  );
}
