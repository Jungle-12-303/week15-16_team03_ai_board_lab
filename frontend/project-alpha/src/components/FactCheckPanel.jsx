export default function FactCheckPanel({
  title,
  result,
  error,
  isLoading,
  onCheck,
  metaItems,
  externalFactTitle,
}) {
  const hasResult = result !== null;
  const isChecked = result?.status === 'CHECKED';
  const verdictLabel = formatVerdict(result?.verdict ?? '');
  const visibleMetaItems = metaItems.filter((item) => item.value.length > 0);

  return (
    <section className="box fact-check-panel">
      <div className="box-header">
        <span>{title}</span>
        <button
          type="button"
          className="plain-button"
          onClick={onCheck}
          disabled={isLoading}
        >
          {isLoading ? 'Checking...' : 'Check'}
        </button>
      </div>

      <div className="fact-check-body">
        {error.length > 0 && <p className="feed-status error-status">{error}</p>}

        {!hasResult && error.length === 0 && (
          <p className="feed-status">No fact check result yet.</p>
        )}

        {hasResult && (
          <div className="fact-check-result">
            <p className="feed-status">{result.message}</p>

            {isChecked && (
              <>
                <div className="fact-check-block">
                  <h3>Claim</h3>
                  <p>{result.claim || 'No specific claim was extracted.'}</p>
                </div>

                <div className="fact-check-block">
                  <h3>Verdict</h3>
                  <span className={`fact-check-verdict ${result.verdict}`}>
                    {verdictLabel}
                  </span>
                </div>

                <div className="fact-check-block">
                  <h3>Comparison</h3>
                  <p>{result.comparison || result.judgement}</p>
                </div>

                <div className="fact-check-block">
                  <h3>Suggestion</h3>
                  <p>{result.suggestion || 'No safer wording was suggested.'}</p>
                </div>

                {visibleMetaItems.length > 0 && (
                  <dl className="fact-check-meta">
                    {visibleMetaItems.map((item) => (
                      <div key={item.label}>
                        <dt>{item.label}</dt>
                        <dd>{item.value}</dd>
                      </div>
                    ))}
                  </dl>
                )}

                <div className="fact-check-block">
                  <h3>{externalFactTitle}</h3>
                  <pre>{result.externalFact}</pre>
                </div>
              </>
            )}
          </div>
        )}
      </div>
    </section>
  );
}

function formatVerdict(verdict) {
  const labels = {
    supported: 'Supported',
    contradicted: 'Contradicted',
    uncertain: 'Uncertain',
    too_vague: 'Too vague',
  };

  return labels[verdict] ?? 'Uncertain';
}
