import { categories } from '../../constants/board';

export default function PostFilterBar({
  selectedCategory,
  selectedTag,
  onSelectCategory,
  onTagChange,
  onResetFilters,
  onOpenComposer,
}) {
  return (
    <section className="box">
      <div className="filterbar">
        <select
          className="select"
          value={selectedCategory}
          onChange={(event) => onSelectCategory(event.target.value)}
        >
          <option value="All">All categories</option>
          {categories.map((categoryName) => (
            <option key={categoryName} value={categoryName}>
              {categoryName}
            </option>
          ))}
        </select>

        <input
          className="input"
          value={selectedTag}
          onChange={(event) => onTagChange(event.target.value)}
          placeholder="Tag filter"
        />

        <button type="button" className="plain-button" onClick={onResetFilters}>
          Reset
        </button>

        <button type="button" className="primary-button" onClick={onOpenComposer}>
          Write post
        </button>
      </div>
    </section>
  );
}
