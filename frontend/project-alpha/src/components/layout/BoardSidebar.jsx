import AgentRecommendationsPanel from '../ai/AgentRecommendationsPanel';
import { categories } from '../../constants/board';

export default function BoardSidebar({
  posts,
  categoryCounts,
  selectedCategory,
  agentRecommendations,
  onSelectCategory,
}) {
  const hasServerCounts = Object.keys(categoryCounts).length > 0;

  function countPostsByCategory(categoryName) {
    if (hasServerCounts) {
      return categoryCounts[categoryName] ?? 0;
    }

    return posts.filter((post) => post.category === categoryName).length;
  }

  const totalPostCount = hasServerCounts
    ? categories.reduce((total, categoryName) => total + countPostsByCategory(categoryName), 0)
    : posts.length;

  return (
    <aside className="sidebar">
      <section className="box">
        <div className="box-header">Board</div>
        <ul className="nav-list">
          <li className={selectedCategory === 'All' ? 'active' : undefined}>
            <button type="button" onClick={() => onSelectCategory('All')}>
              All
            </button>
            <span className="count">{totalPostCount}</span>
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

      <AgentRecommendationsPanel
        recommendations={agentRecommendations.recommendations}
        summary={agentRecommendations.summary}
        steps={agentRecommendations.steps}
        isLoading={agentRecommendations.isLoadingAgentRecommendations}
        error={agentRecommendations.agentRecommendationsError}
        hasLoaded={agentRecommendations.hasLoadedAgentRecommendations}
        onLoad={agentRecommendations.loadMissedPostRecommendations}
      />
    </aside>
  );
}
