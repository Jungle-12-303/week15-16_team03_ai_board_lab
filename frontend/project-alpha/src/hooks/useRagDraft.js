import { useState } from 'react';
import { createDraftFromSources, findSimilarPosts } from '../api/ai/ragApi';

export default function useRagDraft(currentUser) {
  const [similarPosts, setSimilarPosts] = useState([]);
  const [similarPostsError, setSimilarPostsError] = useState('');
  const [isLoadingSimilarPosts, setIsLoadingSimilarPosts] = useState(false);
  const [hasSearchedSimilarPosts, setHasSearchedSimilarPosts] = useState(false);
  const [draftError, setDraftError] = useState('');
  const [draftMessage, setDraftMessage] = useState('');
  const [isGeneratingDraft, setIsGeneratingDraft] = useState(false);

  async function loadSimilarPosts(input) {
    if (currentUser === null) {
      return;
    }

    try {
      setIsLoadingSimilarPosts(true);
      setSimilarPostsError('');

      const nextSimilarPosts = await findSimilarPosts(input);

      setSimilarPosts(nextSimilarPosts);
      setHasSearchedSimilarPosts(true);
    } catch (error) {
      setSimilarPosts([]);
      setSimilarPostsError(error instanceof Error ? error.message : 'Similar posts could not be loaded.');
      setHasSearchedSimilarPosts(true);
    } finally {
      setIsLoadingSimilarPosts(false);
    }
  }

  async function generateDraft(input) {
    if (currentUser === null) {
      return null;
    }

    try {
      setIsGeneratingDraft(true);
      setDraftError('');
      setDraftMessage('');
      setSimilarPostsError('');

      const draftResult = await createDraftFromSources(input);

      setSimilarPosts(draftResult.sources);
      setDraftMessage(draftResult.message);
      setHasSearchedSimilarPosts(true);

      return draftResult;
    } catch (error) {
      setDraftMessage('');
      setDraftError(error instanceof Error ? error.message : 'Draft could not be generated from related posts.');
      return null;
    } finally {
      setIsGeneratingDraft(false);
    }
  }

  function resetRagDraft() {
    setSimilarPosts([]);
    setSimilarPostsError('');
    setIsLoadingSimilarPosts(false);
    setHasSearchedSimilarPosts(false);
    setDraftError('');
    setDraftMessage('');
    setIsGeneratingDraft(false);
  }

  return {
    similarPosts,
    similarPostsError,
    isLoadingSimilarPosts,
    hasSearchedSimilarPosts,
    draftError,
    draftMessage,
    isGeneratingDraft,
    loadSimilarPosts,
    generateDraft,
    resetRagDraft,
  };
}
