import { useState } from 'react';
import { recommendMissedPosts } from '../api/ai/agentApi';

const emptyAgentResult = {
  message: '',
  summary: '',
  recommendations: [],
  steps: [],
};

export default function useAgentRecommendations(currentUser) {
  const [agentResult, setAgentResult] = useState(emptyAgentResult);
  const [isLoadingAgentRecommendations, setIsLoadingAgentRecommendations] = useState(false);
  const [agentRecommendationsError, setAgentRecommendationsError] = useState('');
  const [hasLoadedAgentRecommendations, setHasLoadedAgentRecommendations] = useState(false);

  async function loadMissedPostRecommendations() {
    if (currentUser === null) {
      return null;
    }

    try {
      setIsLoadingAgentRecommendations(true);
      const nextAgentResult = await recommendMissedPosts({
        limit: 5,
      });

      setAgentResult(nextAgentResult);
      setAgentRecommendationsError('');
      setHasLoadedAgentRecommendations(true);
      return nextAgentResult;
    } catch {
      setAgentResult(emptyAgentResult);
      setAgentRecommendationsError('Recommendations are unavailable.');
      setHasLoadedAgentRecommendations(true);
      return null;
    } finally {
      setIsLoadingAgentRecommendations(false);
    }
  }

  function resetAgentRecommendations() {
    setAgentResult(emptyAgentResult);
    setAgentRecommendationsError('');
    setHasLoadedAgentRecommendations(false);
  }

  return {
    ...agentResult,
    isLoadingAgentRecommendations,
    agentRecommendationsError,
    hasLoadedAgentRecommendations,
    loadMissedPostRecommendations,
    resetAgentRecommendations,
  };
}
