export default function WeatherFactCheckPanel({
  result,
  error,
  isLoading,
  onCheck,
}) {
  const hasResult = result !== null;
  const isChecked = result?.status === 'CHECKED';

  return (
    <section className="box fact-check-panel">
      <div className="box-header">
        <span>MCP Weather fact check</span>
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
                  <h3>Post comparison</h3>
                  <p>{result.judgement}</p>
                </div>

                <dl className="fact-check-meta">
                  <div>
                    <dt>Tool</dt>
                    <dd>{result.toolName}</dd>
                  </div>
                  <div>
                    <dt>Location</dt>
                    <dd>{result.location}</dd>
                  </div>
                  <div>
                    <dt>Source</dt>
                    <dd>{result.source}</dd>
                  </div>
                  <div>
                    <dt>Observed</dt>
                    <dd>{result.observedAt}</dd>
                  </div>
                </dl>

                <div className="fact-check-block">
                  <h3>Fetched weather data</h3>
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
