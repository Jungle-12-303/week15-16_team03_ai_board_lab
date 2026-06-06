import { categories } from '../constants/board';

export default function BoardSidebar({ posts, selectedCategory, onSelectCategory }) {
  function countPostsByCategory(categoryName) {
    return posts.filter((post) => post.category === categoryName).length;
  }

  return (
    <aside className="sidebar">
      <section className="box">
        <div className="box-header">Board</div>
        <ul className="nav-list">
          <li className={selectedCategory === 'All' ? 'active' : undefined}>
            <button type="button" onClick={() => onSelectCategory('All')}>
              All
            </button>
            <span className="count">{posts.length}</span>
          </li>

          {categories.map((categoryName) => (
            <li
              key={categoryName}
              className={selectedCategory === categoryName ? 'active' : undefined}
            >
              <button type="button" onClick={() => onSelectCategory(categoryName)}>
                {categoryName}
              </button>
              <span className="count">{countPostsByCategory(categoryName)}</span>
            </li>
          ))}
        </ul>
      </section>
    </aside>
  );
}
