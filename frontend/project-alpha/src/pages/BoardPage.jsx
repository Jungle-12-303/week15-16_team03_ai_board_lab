import BoardSidebar from '../components/BoardSidebar';
import ComposerModal from '../components/ComposerModal';
import Pagination from '../components/Pagination';
import PostFilterBar from '../components/PostFilterBar';
import PostList from '../components/PostList';
import Topbar from '../components/Topbar';
import { categories } from '../constants/board';

export default function BoardPage({
  currentUser,
  posts,
  isLoadingPosts,
  postsError,
  categoryCounts,
  paginatedPosts,
  currentPage,
  totalPages,
  filteredPostCount,
  agentRecommendations,
  searchTerm,
  selectedCategory,
  selectedTag,
  isComposerOpen,
  category,
  title,
  content,
  tagInput,
  similarPosts,
  similarPostsError,
  isLoadingSimilarPosts,
  hasSearchedSimilarPosts,
  draftError,
  draftMessage,
  isGeneratingDraft,
  canSubmit,
  isEditing,
  onLogout,
  onLogoutAll,
  onSearchChange,
  onSelectCategory,
  onTagChange,
  onResetFilters,
  onOpenComposer,
  onPageChange,
  onCategoryChange,
  onTitleChange,
  onContentChange,
  onTagInputChange,
  onFindSimilarPosts,
  onCreateDraftFromSources,
  onSubmit,
  onCloseComposer,
}) {
  return (
    <>
      <Topbar
        currentUser={currentUser}
        onLogout={onLogout}
        onLogoutAll={onLogoutAll}
        searchTerm={searchTerm}
        onSearchChange={onSearchChange}
      />

      <main className="layout">
        <BoardSidebar
          posts={posts}
          categoryCounts={categoryCounts}
          selectedCategory={selectedCategory}
          agentRecommendations={agentRecommendations}
          onSelectCategory={onSelectCategory}
        />

        <section className="main-content">
          <PostFilterBar
            selectedCategory={selectedCategory}
            selectedTag={selectedTag}
            onSelectCategory={onSelectCategory}
            onTagChange={onTagChange}
            onResetFilters={onResetFilters}
            onOpenComposer={onOpenComposer}
          />

          <p className="feed-status">
            Page {currentPage} of {totalPages} - {filteredPostCount} posts
          </p>
          {isLoadingPosts && <p className="feed-status">Loading posts from server...</p>}
          {postsError.length > 0 && (
            <p className="feed-status error-status">{postsError}</p>
          )}

          <PostList posts={paginatedPosts} />

          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={onPageChange}
          />
        </section>
      </main>

      {isComposerOpen && (
        <ComposerModal
          categories={categories}
          category={category}
          title={title}
          content={content}
          tagInput={tagInput}
          similarPosts={similarPosts}
          similarPostsError={similarPostsError}
          isLoadingSimilarPosts={isLoadingSimilarPosts}
          hasSearchedSimilarPosts={hasSearchedSimilarPosts}
          draftError={draftError}
          draftMessage={draftMessage}
          isGeneratingDraft={isGeneratingDraft}
          canSubmit={canSubmit}
          isEditing={isEditing}
          onCategoryChange={onCategoryChange}
          onTitleChange={onTitleChange}
          onContentChange={onContentChange}
          onTagInputChange={onTagInputChange}
          onFindSimilarPosts={onFindSimilarPosts}
          onCreateDraftFromSources={onCreateDraftFromSources}
          onSubmit={onSubmit}
          onClose={onCloseComposer}
        />
      )}
    </>
  );
}
