import PostForm from './PostForm';

export default function ComposerModal({
  categories,
  category,
  title,
  content,
  tagInput,
  canSubmit,
  isEditing,
  onCategoryChange,
  onTitleChange,
  onContentChange,
  onTagInputChange,
  onSubmit,
  onClose,
}) {
  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <section
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-label={isEditing ? 'Edit post dialog' : 'Write post dialog'}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="box-header">
          <span>{isEditing ? 'Edit post' : 'New post'}</span>
          <button type="button" className="plain-button" onClick={onClose}>
            Close
          </button>
        </div>

        <PostForm
          categories={categories}
          category={category}
          title={title}
          content={content}
          tagInput={tagInput}
          canSubmit={canSubmit}
          onCategoryChange={onCategoryChange}
          onTitleChange={onTitleChange}
          onContentChange={onContentChange}
          onTagInputChange={onTagInputChange}
          onSubmit={onSubmit}
          isEditing={isEditing}
          onCancelEdit={onClose}
        />
      </section>
    </div>
  );
}
