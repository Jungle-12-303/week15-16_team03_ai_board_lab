import { useState } from 'react';

const defaultCategory = 'Learning';

export default function usePostComposer() {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [tagInput, setTagInput] = useState('');
  const [category, setCategory] = useState(defaultCategory);
  const [editingPostId, setEditingPostId] = useState(null);
  const [isComposerOpen, setIsComposerOpen] = useState(false);

  const canSubmit = title.trim().length > 0 && content.trim().length > 0;

  function openComposer() {
    setIsComposerOpen(true);
  }

  function startEditPost(post) {
    setEditingPostId(post.id);
    setTitle(post.title);
    setContent(post.content);
    setCategory(post.category);
    setTagInput(post.tags.join(', '));
    setIsComposerOpen(true);
  }

  function resetComposer() {
    setEditingPostId(null);
    setTitle('');
    setContent('');
    setTagInput('');
    setCategory(defaultCategory);
    setIsComposerOpen(false);
  }

  function clearAfterSave() {
    setEditingPostId(null);
    setTitle('');
    setContent('');
    setTagInput('');
    setIsComposerOpen(false);
  }

  function getTags() {
    return parseTagInput(tagInput);
  }

  return {
    title,
    content,
    tagInput,
    category,
    editingPostId,
    isComposerOpen,
    canSubmit,
    setTitle,
    setContent,
    setTagInput,
    setCategory,
    openComposer,
    startEditPost,
    resetComposer,
    clearAfterSave,
    getTags,
  };
}

function parseTagInput(input) {
  return input
    .split(',')
    .map((tag) => tag.trim())
    .filter((tag) => tag.length > 0);
}
