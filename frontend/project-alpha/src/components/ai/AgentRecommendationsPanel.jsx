import { Link } from 'react-router-dom';

export default function AgentRecommendationsPanel({
  recommendations,
  summary,
  steps,
  isLoading,
  error,
  hasLoaded,
  onLoad,
}) {
  return (
    <section className="box">
      <div className="box-header">Missed posts</div>
      <div className="agent-panel">
        <button
          type="button"
          className="plain-button full-width"
          disabled={isLoading}
          onClick={onLoad}
        >
          {isLoading ? 'Checking...' : 'Recommend 5 posts'}
        </button>

        {error.length > 0 && <p className="feed-status error-status">{error}</p>}

        {summary.length > 0 && <p className="agent-summary">{summary}</p>}

        {hasLoaded && recommendations.length === 0 && error.length === 0 && (
          <p className="feed-status">No unread recommendations yet.</p>
        )}

        {recommendations.length > 0 && (
          <ol className="agent-list">
            {recommendations.map((recommendation) => (
              <li key={recommendation.postId}>
                <Link to={`/posts/${recommendation.postId}`}>{recommendation.title}</Link>
                <span>
                  {recommendation.category} · score {formatScore(recommendation.score)}
                </span>
                <p>{recommendation.reason}</p>
                {recommendation.scoreBreakdown && (
                  <div className="agent-score-breakdown">
                    <span>
                      Category {formatScore(recommendation.scoreBreakdown.categoryContribution)}
                    </span>
                    <span>
                      Tag {formatScore(recommendation.scoreBreakdown.tagContribution)}
                    </span>
                    <span>
                      Recency {formatScore(recommendation.scoreBreakdown.recencyContribution)}
                    </span>
                    {recommendation.scoreBreakdown.matchedTags.length > 0 && (
                      <span>
                        Matched {recommendation.scoreBreakdown.matchedTags.join(', ')}
                      </span>
                    )}
                  </div>
                )}
              </li>
            ))}
          </ol>
        )}

        {steps.length > 0 && (
          <details className="agent-trace">
            <summary>Trace</summary>
            <ol>
              {steps.map((step) => (
                <li key={`${step.tool}-${step.observation}`}>
                  <strong>{step.tool}</strong>
                  <span>{step.observation}</span>
                </li>
              ))}
            </ol>
          </details>
        )}
      </div>
    </section>
  );
}

function formatScore(score) {
  return Number(score).toFixed(3);
}
