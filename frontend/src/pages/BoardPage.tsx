import type { KeyboardEvent } from 'react'
import { Link } from 'react-router-dom'
import type { Comment, McpWeatherDraftResponse, Post, RagAnswerResponse, RagStatusResponse } from '../types'

type BoardPageProps = {
  posts: Post[]
  selectedPostId: number | null
  selectedPost: Post | null
  title: string
  content: string
  editingPostId: number | null
  isPostEditorOpen: boolean
  comments: Comment[]
  commentContent: string
  editingCommentId: number | null
  tagInput: string
  tagNames: string[]
  keyword: string
  page: number
  totalPages: number
  isLoggedIn: boolean
  currentNickname: string
  currentLoginId: string
  mcpCity: string
  mcpForecastDays: string
  mcpResult: McpWeatherDraftResponse | null
  mcpError: string
  isMcpLoading: boolean
  ragQuestion: string
  ragResult: RagAnswerResponse | null
  ragError: string
  isRagLoading: boolean
  isRagReindexing: boolean
  ragStatusMessage: string
  ragSystemStatus: RagStatusResponse | null
  onTitleChange: (value: string) => void
  onContentChange: (value: string) => void
  onCommentContentChange: (value: string) => void
  onTagInputChange: (value: string) => void
  onSearchKeywordChange: (value: string) => void
  onMcpCityChange: (value: string) => void
  onMcpForecastDaysChange: (value: string) => void
  onRagQuestionChange: (value: string) => void
  onSelectPost: (id: number) => void
  onStartEditPost: (post: Post) => void
  onCreatePost: () => void
  onUpdatePost: () => void
  onDeletePost: (id: number) => void
  onTagKeyDown: (event: KeyboardEvent<HTMLInputElement>) => void
  onRemoveTag: (tag: string) => void
  onSearch: () => void
  onAskMcpWeatherDraft: () => void
  onAskRag: () => void
  onReindexPosts: () => void
  onPageChange: (page: number) => void
  onCreateComment: () => void
  onStartEditComment: (comment: Comment) => void
  onUpdateComment: () => void
  onDeleteComment: (id: number) => void
  onLogout: () => void
  onBackToList: () => void
  onOpenCreatePost: () => void
  onClosePostEditor: () => void
  onUseMcpDraft: () => void
}

function formatDate(createdAt: string) {
  return new Date(createdAt).toLocaleString('ko-KR')
}

function BoardPage({
  posts,
  selectedPostId,
  selectedPost,
  title,
  content,
  editingPostId,
  isPostEditorOpen,
  comments,
  commentContent,
  editingCommentId,
  tagInput,
  tagNames,
  keyword,
  page,
  totalPages,
  isLoggedIn,
  currentNickname,
  currentLoginId,
  mcpCity,
  mcpForecastDays,
  mcpResult,
  mcpError,
  isMcpLoading,
  ragQuestion,
  ragResult,
  ragError,
  isRagLoading,
  isRagReindexing,
  ragStatusMessage,
  ragSystemStatus,
  onTitleChange,
  onContentChange,
  onCommentContentChange,
  onTagInputChange,
  onSearchKeywordChange,
  onMcpCityChange,
  onMcpForecastDaysChange,
  onRagQuestionChange,
  onSelectPost,
  onStartEditPost,
  onCreatePost,
  onUpdatePost,
  onDeletePost,
  onTagKeyDown,
  onRemoveTag,
  onSearch,
  onAskMcpWeatherDraft,
  onAskRag,
  onReindexPosts,
  onPageChange,
  onCreateComment,
  onStartEditComment,
  onUpdateComment,
  onDeleteComment,
  onLogout,
  onBackToList,
  onOpenCreatePost,
  onClosePostEditor,
  onUseMcpDraft,
}: BoardPageProps) {
  const safeTotalPages = totalPages === 0 ? 1 : totalPages
  const isDetailMode = selectedPostId !== null

  return (
    <div className="board-page">
      <header className="topbar">
        <div>
          <p className="page-kicker">AI Board Lab</p>
          <h1 className="page-title">게시글 게시판</h1>
          <p className="page-description">로그인과 게시판 기능을 분리한 실제 서비스 형태의 구조입니다.</p>
        </div>

        <div className="topbar-actions">
          <Link className="nav-link" to="/posts">
            게시글 목록
          </Link>

          {isLoggedIn ? (
            <>
              <span className="user-badge">{currentNickname}</span>
              <button className="secondary-button" onClick={onLogout} type="button">
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link className="nav-link" to="/login">
                로그인
              </Link>
              <Link className="secondary-link-button" to="/signup">
                회원가입
              </Link>
            </>
          )}
        </div>
      </header>

      <main className="board-content">
        {isDetailMode ? (
          <section className="detail-layout">
            <div className="panel-card detail-view-card">
              <div className="detail-toolbar">
                <button className="secondary-button" onClick={onBackToList} type="button">
                  목록으로 돌아가기
                </button>

                {selectedPost !== null && selectedPost.ownerLoginId === currentLoginId && (
                  <div className="card-actions">
                    <button
                      className="secondary-button"
                      onClick={() => {
                        onStartEditPost(selectedPost)
                        onBackToList()
                      }}
                      type="button"
                    >
                      수정하기
                    </button>
                    <button className="danger-button" onClick={() => onDeletePost(selectedPost.id)} type="button">
                      삭제하기
                    </button>
                  </div>
                )}
              </div>

              {selectedPost !== null ? (
                <>
                  <div className="section-header">
                    <p className="section-kicker">Detail</p>
                    <h2>{selectedPost.title}</h2>
                  </div>

                  <p className="meta-text">
                    {selectedPost.authorName} · {formatDate(selectedPost.createdAt)}
                  </p>
                  <p className="detail-content">{selectedPost.content}</p>

                  <div className="tag-list">
                    {selectedPost.tags.map((tag) => (
                      <span className="tag-chip" key={tag.id}>
                        {tag.name}
                      </span>
                    ))}
                  </div>

                  <div className="comment-section">
                    <div className="section-header">
                      <p className="section-kicker">Comment</p>
                      <h3>댓글</h3>
                    </div>

                    <ul className="comment-list">
                      {comments.map((comment) => (
                        <li className="comment-item" key={comment.id}>
                          <div>
                            <p className="comment-author">{comment.authorName}</p>
                            <p className="comment-content">{comment.content}</p>
                          </div>

                          {comment.ownerLoginId === currentLoginId && (
                            <div className="card-actions">
                              <button
                                className="secondary-button"
                                onClick={() => onStartEditComment(comment)}
                                type="button"
                              >
                                수정
                              </button>
                              <button className="danger-button" onClick={() => onDeleteComment(comment.id)} type="button">
                                삭제
                              </button>
                            </div>
                          )}
                        </li>
                      ))}
                    </ul>

                    {isLoggedIn ? (
                      <div className="comment-editor">
                        <textarea
                          className="text-area"
                          placeholder="댓글 내용"
                          value={commentContent}
                          onChange={(event) => onCommentContentChange(event.target.value)}
                        />
                        <button
                          className="primary-button"
                          onClick={editingCommentId === null ? onCreateComment : onUpdateComment}
                          type="button"
                        >
                          {editingCommentId === null ? '댓글 작성' : '댓글 수정'}
                        </button>
                      </div>
                    ) : (
                      <p className="helper-text">댓글 작성은 로그인 후 사용할 수 있습니다.</p>
                    )}
                  </div>
                </>
              ) : (
                <div className="empty-detail">
                  <p className="section-kicker">Detail</p>
                  <h2>게시글을 불러오는 중입니다.</h2>
                  <p className="page-description">잠시만 기다리면 상세 내용과 댓글이 표시됩니다.</p>
                </div>
              )}
            </div>
          </section>
        ) : isPostEditorOpen ? (
          <section className="editor-layout">
            <div className="panel-card editor-view-card">
              <div className="detail-toolbar">
                <button className="secondary-button" onClick={onClosePostEditor} type="button">
                  목록으로 돌아가기
                </button>
              </div>

              <div className="section-header">
                <p className="section-kicker">Write</p>
                <h2>{editingPostId === null ? '게시글 작성' : '게시글 수정'}</h2>
              </div>

              {isLoggedIn ? (
                <div className="editor-form">
                  <input
                    className="text-input"
                    type="text"
                    placeholder="제목"
                    value={title}
                    onChange={(event) => onTitleChange(event.target.value)}
                  />

                  <textarea
                    className="text-area"
                    placeholder="내용"
                    value={content}
                    onChange={(event) => onContentChange(event.target.value)}
                  />

                  <input
                    className="text-input"
                    type="text"
                    placeholder="태그 입력 후 Enter"
                    value={tagInput}
                    onChange={(event) => onTagInputChange(event.target.value)}
                    onKeyDown={onTagKeyDown}
                  />

                  <div className="tag-list">
                    {tagNames.map((tag) => (
                      <span className="tag-chip" key={tag}>
                        {tag}
                        <button className="tag-remove-button" onClick={() => onRemoveTag(tag)} type="button">
                          x
                        </button>
                      </span>
                    ))}
                  </div>

                  <p className="helper-text">작성자는 현재 로그인한 닉네임으로 자동 저장됩니다.</p>

                  <button
                    className="primary-button"
                    onClick={editingPostId === null ? onCreatePost : onUpdatePost}
                    type="button"
                  >
                    {editingPostId === null ? '게시글 작성' : '게시글 수정'}
                  </button>
                </div>
              ) : (
                <div className="locked-message">
                  <p>게시글 작성은 로그인 후 사용할 수 있습니다.</p>
                  <div className="locked-actions">
                    <Link className="nav-link" to="/login">
                      로그인
                    </Link>
                    <Link className="secondary-link-button" to="/signup">
                      회원가입
                    </Link>
                  </div>
                </div>
              )}
            </div>
          </section>
        ) : (
          <>
            <section className="board-toolbar">
              <div className="panel-card rag-card">
                <div className="section-header">
                  <p className="section-kicker">AI Search</p>
                  <h2>AI 게시글 검색 도우미</h2>
                </div>

                {isLoggedIn ? (
                  <div className="rag-layout">
                    <div className="search-row rag-search-row">
                      <input
                        className="text-input"
                        type="text"
                        placeholder="예: JWT 로그인 구현할 때 필요한 흐름이 뭐야?"
                        value={ragQuestion}
                        onChange={(event) => onRagQuestionChange(event.target.value)}
                        onKeyDown={(event) => {
                          if (event.key === 'Enter') {
                            onAskRag()
                          }
                        }}
                      />
                      <button className="primary-button" onClick={onAskRag} type="button" disabled={isRagLoading}>
                        {isRagLoading ? '검색 중...' : 'AI 검색'}
                      </button>
                    </div>

                    <p className="helper-text">
                      자연어 질문을 입력하면 관련 게시글을 찾아 짧게 요약해서 답변합니다.
                    </p>

                    {ragSystemStatus !== null && (
                      <p className="meta-text">
                        API 키: {ragSystemStatus.apiKeyConfigured ? '설정됨' : '미설정'} · 임베딩 모델:{' '}
                        {ragSystemStatus.embeddingModel} · 채팅 모델: {ragSystemStatus.chatModel} · 인덱싱된 게시글:{' '}
                        {ragSystemStatus.indexedPostCount}개 · 임베딩 행: {ragSystemStatus.embeddingRowCount}개
                      </p>
                    )}

                    <div className="rag-actions">
                      <button
                        className="secondary-button"
                        onClick={onReindexPosts}
                        type="button"
                        disabled={isRagReindexing}
                      >
                        {isRagReindexing ? '임베딩 갱신 중...' : '기존 글 임베딩 갱신'}
                      </button>
                    </div>

                    {ragError !== '' && <p className="rag-error">{ragError}</p>}
                    {ragStatusMessage !== '' && <p className="rag-status">{ragStatusMessage}</p>}

                    {ragResult !== null && (
                      <div className="rag-result-card">
                        <div className="section-header compact">
                          <p className="section-kicker">Answer</p>
                          <h3>AI 답변</h3>
                        </div>
                        <p className="detail-content">{ragResult.answer}</p>
                        <p className="meta-text">
                          답변 모델: {ragResult.answerModel} · 임베딩 모델: {ragResult.embeddingModel}
                        </p>

                        <div className="section-header compact">
                          <p className="section-kicker">Reference</p>
                          <h3>참고 게시글</h3>
                        </div>

                        <ul className="rag-reference-list">
                          {ragResult.references.map((reference) => (
                            <li className="rag-reference-item" key={reference.postId}>
                              <button
                                className="post-title-button"
                                onClick={() => onSelectPost(reference.postId)}
                                type="button"
                              >
                                {reference.title}
                              </button>
                              <p className="meta-text">
                                {reference.authorName} · {formatDate(reference.createdAt)} · 유사도 {reference.similarityScore.toFixed(3)}
                              </p>
                              <p className="comment-content">{reference.contentPreview}</p>
                              <div className="tag-list compact">
                                {reference.tags.map((tag) => (
                                  <span className="tag-chip compact" key={`${reference.postId}-${tag.id}`}>
                                    {tag.name}
                                  </span>
                                ))}
                              </div>
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}

                    <div className="rag-result-card">
                      <div className="section-header compact">
                        <p className="section-kicker">MCP Tool</p>
                        <h3>날씨 브리핑 초안 생성</h3>
                      </div>

                      <div className="search-row rag-search-row mcp-input-row">
                        <input
                          className="text-input"
                          type="text"
                          placeholder="도시 이름 예: Seoul, Busan, Tokyo"
                          value={mcpCity}
                          onChange={(event) => onMcpCityChange(event.target.value)}
                          onKeyDown={(event) => {
                            if (event.key === 'Enter') {
                              onAskMcpWeatherDraft()
                            }
                          }}
                        />
                        <input
                          className="text-input mcp-days-input"
                          type="number"
                          min="1"
                          max="3"
                          value={mcpForecastDays}
                          onChange={(event) => onMcpForecastDaysChange(event.target.value)}
                        />
                        <button
                          className="primary-button"
                          onClick={onAskMcpWeatherDraft}
                          type="button"
                          disabled={isMcpLoading}
                        >
                          {isMcpLoading ? '초안 생성 중...' : '날씨 초안 생성'}
                        </button>
                      </div>

                      <p className="helper-text">
                        MCP 도구가 외부 날씨 데이터를 조회해서 게시글 제목, 본문, 태그 초안을 만들어줍니다.
                      </p>

                      {mcpError !== '' && <p className="rag-error">{mcpError}</p>}

                      {mcpResult !== null && (
                        <>
                          <div className="section-header compact">
                            <p className="section-kicker">Draft</p>
                            <h3>MCP 생성 결과</h3>
                          </div>

                          <p className="meta-text">
                            요청자: {mcpResult.requestedBy} · 도시: {mcpResult.city} · 예보 일수: {mcpResult.forecastDays}
                          </p>

                          <div>
                            <p className="comment-author">{mcpResult.title}</p>
                            <p className="detail-content">{mcpResult.content}</p>
                          </div>

                          <div className="tag-list">
                            {mcpResult.tags.map((tag) => (
                              <span className="tag-chip" key={tag}>
                                {tag}
                              </span>
                            ))}
                          </div>

                          <div className="section-header compact">
                            <p className="section-kicker">Source</p>
                            <h3>참고 데이터</h3>
                          </div>

                          <ul className="rag-reference-list">
                            {mcpResult.sourceSummary.map((line) => (
                              <li className="rag-reference-item" key={line}>
                                <p className="comment-content">{line}</p>
                              </li>
                            ))}
                          </ul>

                          <div className="rag-actions">
                            <button className="secondary-button" onClick={onUseMcpDraft} type="button">
                              이 초안으로 게시글 작성하기
                            </button>
                          </div>
                        </>
                      )}
                    </div>
                  </div>
                ) : (
                  <p className="helper-text">AI 검색은 로그인 후 사용할 수 있습니다.</p>
                )}
              </div>

              <div className="panel-card search-card">
                <div className="section-header">
                  <p className="section-kicker">Search</p>
                  <h2>게시글 검색</h2>
                </div>

                <div className="search-row">
                  <input
                    className="text-input"
                    type="text"
                    placeholder="제목이나 내용으로 검색하세요"
                    value={keyword}
                    onChange={(event) => onSearchKeywordChange(event.target.value)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') {
                        onSearch()
                      }
                    }}
                  />
                  <button className="primary-button" onClick={onSearch} type="button">
                    검색
                  </button>
                </div>
              </div>
            </section>

            <section className="list-layout">
              <div className="panel-card list-card">
                <div className="list-header-row">
                  <div className="section-header">
                    <p className="section-kicker">Board</p>
                    <h2>게시글 목록</h2>
                  </div>

                  <div className="list-header-actions">
                    {isLoggedIn ? (
                      <button className="primary-button" onClick={onOpenCreatePost} type="button">
                        게시글 작성
                      </button>
                    ) : (
                      <Link className="secondary-link-button" to="/login">
                        로그인 후 작성
                      </Link>
                    )}
                  </div>
                </div>

                <ul className="post-list">
                  {posts.map((post) => (
                    <li key={post.id}>
                      <article className={selectedPostId === post.id ? 'post-card selected' : 'post-card'}>
                        <button className="post-title-button" onClick={() => onSelectPost(post.id)} type="button">
                          {post.title}
                        </button>
                        <p className="meta-text">
                          {post.authorName} · {formatDate(post.createdAt)}
                        </p>

                        <div className="tag-list compact">
                          {post.tags.map((tag) => (
                            <span className="tag-chip compact" key={tag.id}>
                              {tag.name}
                            </span>
                          ))}
                        </div>

                        {post.ownerLoginId === currentLoginId && (
                          <div className="card-actions">
                            <button className="secondary-button" onClick={() => onStartEditPost(post)} type="button">
                              수정
                            </button>
                            <button className="danger-button" onClick={() => onDeletePost(post.id)} type="button">
                              삭제
                            </button>
                          </div>
                        )}
                      </article>
                    </li>
                  ))}
                </ul>

                <div className="pagination">
                  <button
                    className="secondary-button"
                    disabled={page === 0}
                    onClick={() => onPageChange(page - 1)}
                    type="button"
                  >
                    이전
                  </button>
                  <span className="pagination-status">
                    {page + 1} / {safeTotalPages}
                  </span>
                  <button
                    className="secondary-button"
                    disabled={page + 1 >= totalPages}
                    onClick={() => onPageChange(page + 1)}
                    type="button"
                  >
                    다음
                  </button>
                </div>
              </div>
            </section>
          </>
        )}
      </main>
    </div>
  )
}

export default BoardPage
