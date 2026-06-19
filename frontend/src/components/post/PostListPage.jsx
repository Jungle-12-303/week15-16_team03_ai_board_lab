import { useEffect, useState } from 'react'
import { ExternalLink, FilePlus2, Send, Trash2 } from 'lucide-react'
import { aiApi, getErrorMessage, postApi } from '../../api/client.js'
import PostListItem from './PostListItem.jsx'
import * as styles from '../../css/PostListPage.css'
import Button from '../common/Button.jsx'
import Card from '../common/Card.jsx'

const POSTS_PER_PAGE = 3
const emptyEditor = {
    title: '',
    content: '',
    tagsText: ''
}

const emptyAiState = {
    loading: false,
    error: '',
    ragResult: null,
    mcpResult: null,
    agentResult: null
}

export default function PostListPage({ user, setNotice }) {
    const [posts, setPosts] = useState([])
    const [pageInfo, setPageInfo] = useState({
        page: 0,
        totalPages: 0
    })
    const [query, setQuery] = useState({
        keyword: '',
        tag: ''
    })
    const [mode, setMode] = useState('list')
    const [selectedPost, setSelectedPost] = useState(null)
    const [editingId, setEditingId] = useState(null)
    const [editor, setEditor] = useState(emptyEditor)
    const [commentContent, setCommentContent] = useState('')
    const [mcpUsername, setMcpUsername] = useState('openai')
    const [loading, setLoading] = useState(false)
    const [aiState, setAiState] = useState(emptyAiState)
    const [chatInput, setChatInput] = useState('')
    const [chatMessages, setChatMessages] = useState([])
    const [chatLoading, setChatLoading] = useState(false)

    const isLoggedIn = Boolean(user)
    const pageNumbers = Array.from(
        { length: pageInfo.totalPages },
        (_, pageIndex) => pageIndex
    )

    useEffect(() => {
        loadPosts(0)
    }, [])

    async function loadPosts(page = pageInfo.page) {
        setLoading(true)

        try {
            const data = await postApi.list({
                page,
                size: POSTS_PER_PAGE,
                keyword: query.keyword.trim() || undefined,
                tag: query.tag.trim() || undefined
            })

            setPosts(data.content ?? [])
            setPageInfo({
                page: data.page ?? page,
                totalPages: data.totalPages ?? 0
            })
            setMode('list')
            setSelectedPost(null)
        } catch (error) {
            setNotice?.(getErrorMessage(error))
            console.error(error)
        } finally {
            setLoading(false)
        }
    }

    async function loadPost(id) {
        setLoading(true)

        try {
            const data = await postApi.get(id)
            setSelectedPost(data)
            setCommentContent('')
            setMode('detail')
            setAiState(emptyAiState)
        } catch (error) {
            setNotice?.(getErrorMessage(error))
            console.error(error)
        } finally {
            setLoading(false)
        }
    }

    function handleSearchSubmit(event) {
        event.preventDefault()
        loadPosts(0)
    }

    function backToList() {
        setMode('list')
        setSelectedPost(null)
        setEditingId(null)
        setEditor(emptyEditor)
        setCommentContent('')
        setAiState(emptyAiState)
    }

    function startCreate() {
        if (!isLoggedIn) {
            setNotice?.('로그인 후 게시글을 작성할 수 있습니다.')
            return
        }

        setEditingId(null)
        setEditor(emptyEditor)
        setSelectedPost(null)
        setAiState(emptyAiState)
        setMode('edit')
    }

    function startEdit(post) {
        setEditingId(post.id)
        setEditor({
            title: post.title ?? '',
            content: post.content ?? '',
            tagsText: post.tags?.join(', ') ?? ''
        })
        setAiState(emptyAiState)
        setMode('edit')
    }

    function updateEditor(field, value) {
        setEditor((current) => ({
            ...current,
            [field]: value
        }))
    }

    function toPostPayload() {
        return {
            title: editor.title.trim(),
            content: editor.content.trim(),
            tags: editor.tagsText
                .split(',')
                .map((tag) => tag.trim())
                .filter(Boolean)
        }
    }

    async function handleEditorSubmit(event) {
        event.preventDefault()

        if (!isLoggedIn) {
            setNotice?.('로그인 후 게시글을 저장할 수 있습니다.')
            return
        }

        const payload = toPostPayload()

        if (!payload.title || !payload.content) {
            setNotice?.('제목과 본문을 입력해 주세요.')
            return
        }

        try {
            const savedPost = editingId
                ? await postApi.update(editingId, payload)
                : await postApi.create(payload)

            setNotice?.(editingId ? '게시글을 수정했습니다.' : '게시글을 작성했습니다.')
            setEditor(emptyEditor)
            setEditingId(null)
            await loadPosts(0)
            await loadPost(savedPost.id)
        } catch (error) {
            setNotice?.(getErrorMessage(error))
            console.error(error)
        }
    }

    async function deletePost(id) {
        if (!confirm('게시글을 삭제할까요?')) {
            return
        }

        try {
            await postApi.remove(id)
            setNotice?.('게시글을 삭제했습니다.')
            setSelectedPost(null)
            setMode('list')
            await loadPosts(0)
        } catch (error) {
            setNotice?.(getErrorMessage(error))
            console.error(error)
        }
    }

    async function handleCommentSubmit(event) {
        event.preventDefault()

        if (!isLoggedIn) {
            setNotice?.('로그인 후 댓글을 작성할 수 있습니다.')
            return
        }

        const content = commentContent.trim()

        if (!content) {
            setNotice?.('댓글 내용을 입력해 주세요.')
            return
        }

        try {
            await postApi.comment(selectedPost.id, { content })
            setCommentContent('')
            setNotice?.('댓글을 작성했습니다.')
            await loadPost(selectedPost.id)
        } catch (error) {
            setNotice?.(getErrorMessage(error))
            console.error(error)
        }
    }

    async function deleteComment(commentId) {
        if (!confirm('댓글을 삭제할까요?')) {
            return
        }

        try {
            await postApi.removeComment(commentId)
            setNotice?.('댓글을 삭제했습니다.')
            await loadPost(selectedPost.id)
        } catch (error) {
            setNotice?.(getErrorMessage(error))
            console.error(error)
        }
    }

    async function runSimilar() {
        const sourceText = mode === 'detail' && selectedPost
            ? `${selectedPost.title}\n${selectedPost.content}`
            : `${editor.title}\n${editor.content}`

        if (!sourceText.trim()) {
            setAiState((current) => ({
                ...current,
                error: 'AI 참고 결과를 만들 제목이나 본문이 필요합니다.'
            }))
            return
        }

        setAiState((current) => ({ ...current, loading: true, error: '' }))

        try {
            const data = await aiApi.similar({
                query: sourceText,
                excludePostId: selectedPost?.id
            })
            setAiState((current) => ({
                ...current,
                loading: false,
                ragResult: data
            }))
        } catch (error) {
            setAiState((current) => ({
                ...current,
                loading: false,
                error: getErrorMessage(error)
            }))
        }
    }

    async function runAgent() {
        const draft = `${editor.title}\n${editor.content}`.trim()

        if (!draft) {
            setAiState((current) => ({
                ...current,
                error: '작성 보조를 실행할 제목이나 본문이 필요합니다.'
            }))
            return
        }

        setAiState((current) => ({ ...current, loading: true, error: '' }))

        try {
            const data = await aiApi.agent({
                draft,
                intention: '게시글 작성 보조'
            })
            setAiState((current) => ({
                ...current,
                loading: false,
                agentResult: data
            }))
        } catch (error) {
            setAiState((current) => ({
                ...current,
                loading: false,
                error: getErrorMessage(error)
            }))
        }
    }

    async function runMcp() {
        const username = mcpUsername.trim()

        if (!username) {
            setAiState((current) => ({
                ...current,
                error: 'GitHub username을 입력해 주세요.'
            }))
            return
        }

        setAiState((current) => ({ ...current, loading: true, error: '' }))

        try {
            const data = await aiApi.mcp({
                jsonrpc: '2.0',
                method: 'github.getUser',
                params: { username },
                id: `app2-${Date.now()}`
            })
            setAiState((current) => ({
                ...current,
                loading: false,
                mcpResult: data
            }))
        } catch (error) {
            setAiState((current) => ({
                ...current,
                loading: false,
                error: getErrorMessage(error)
            }))
        }
    }

    async function submitChat(event) {
        event.preventDefault()

        const message = chatInput.trim()

        if (!message) {
            return
        }

        if (!isLoggedIn) {
            setChatMessages((current) => [
                ...current,
                {
                    id: `assistant-login-${Date.now()}`,
                    role: 'assistant',
                    content: '로그인 후 질문할 수 있습니다.',
                    sources: []
                }
            ])
            return
        }

        const messageId = Date.now()
        setChatMessages((current) => [
            ...current,
            {
                id: `user-${messageId}`,
                role: 'user',
                content: message,
                sources: []
            }
        ])
        setChatInput('')
        setChatLoading(true)

        try {
            const data = await aiApi.chat({ message })
            setChatMessages((current) => [
                ...current,
                {
                    id: `assistant-${messageId}`,
                    role: 'assistant',
                    content: data.summary,
                    sources: data.sources ?? [],
                    originalQuestion: message,
                    notionUrl: '',
                    notionError: '',
                    notionSaving: false
                }
            ])
        } catch (error) {
            setChatMessages((current) => [
                ...current,
                {
                    id: `assistant-error-${messageId}`,
                    role: 'assistant',
                    content: getErrorMessage(error),
                    sources: []
                }
            ])
        } finally {
            setChatLoading(false)
        }
    }

    function updateChatMessage(messageId, update) {
        setChatMessages((current) => current.map((message) => (
            message.id === messageId ? { ...message, ...update } : message
        )))
    }

    async function saveChatAnswerToNotion(message) {
        if (!message.originalQuestion || message.notionUrl || message.notionSaving) {
            return
        }

        updateChatMessage(message.id, {
            notionSaving: true,
            notionError: ''
        })

        try {
            const data = await aiApi.mcp({
                jsonrpc: '2.0',
                id: `notion-${Date.now()}`,
                method: 'notion.saveRagAnswer',
                params: {
                    question: message.originalQuestion,
                    answer: message.content,
                    sources: message.sources ?? []
                }
            })

            if (data.error) {
                throw new Error(data.error.message || 'Notion 저장 실패')
            }

            const result = data.result
            if (!result?.saved || !result?.notionUrl) {
                throw new Error('Notion 저장 실패')
            }

            updateChatMessage(message.id, {
                notionSaving: false,
                notionUrl: result.notionUrl,
                notionPageId: result.notionPageId,
                notionError: ''
            })
        } catch (error) {
            updateChatMessage(message.id, {
                notionSaving: false,
                notionError: error?.response ? getErrorMessage(error) : error?.message || 'Notion 저장 실패'
            })
        }
    }

    function applyRecommendedTags() {
        const recommendedTags = aiState.agentResult?.recommendedTags ?? []

        if (recommendedTags.length === 0) {
            setNotice?.('적용할 추천 태그가 없습니다.')
            return
        }

        const currentTags = editor.tagsText
            .split(',')
            .map((tag) => tag.trim())
            .filter(Boolean)
        const mergedTags = Array.from(new Set([...currentTags, ...recommendedTags]))

        updateEditor('tagsText', mergedTags.join(', '))
    }

    function renderAiPanel({ showAgent = false } = {}) {
        const hasResult = aiState.ragResult || aiState.mcpResult || aiState.agentResult || aiState.error || aiState.loading
        const MIN_SIMILARITY_SCORE = 0.5


        if (!hasResult && !showAgent) {
            return null
        }

        const similarSources = (aiState.ragResult?.sources ?? []).filter((source) => (
            typeof source.score === 'number' && source.score >= MIN_SIMILARITY_SCORE
        ))

        return (
            <Card className={styles.aiPanel}>
                <div className={styles.aiHeader}>
                    <h2 className={styles.panelTitle}>AI 참고 결과</h2>
                    <div className={styles.ownerActions}>
                        <Button
                            type="button"
                            size="sm"
                            variant="secondary"
                            disabled={aiState.loading}
                            onClick={runSimilar}
                        >
                            유사 글
                        </Button>

                    </div>
                </div>

                <div className={styles.mcpTool}>
                    <input
                        className={styles.searchInput}
                        value={mcpUsername}
                        onChange={(event) => setMcpUsername(event.target.value)}
                        placeholder="GitHub username"
                    />

                </div>

                {aiState.loading && <p className={styles.loading}>AI 요청 중...</p>}
                {aiState.error && <p className={styles.empty}>{aiState.error}</p>}

                {aiState.ragResult && (
                    <div className={styles.aiResult}>
                        <h3 className={styles.resultTitle}>유사 글 결과</h3>
                        <p className={styles.ragSummary}>{aiState.ragResult.summary}</p>

                        {similarSources.length > 0 ? (
                            <div className={styles.sourceCardList}>
                                {similarSources.map((source) => (
                                    <button
                                        className={styles.sourceCard}
                                        type="button"
                                        key={source.id}
                                        onClick={() => loadPost(source.id)}
                                        aria-label={`${source.title} 게시글로 이동`}
                                    >
                                        <span className={styles.sourceCardTitle}>{source.title}</span>
                                        <span className={styles.sourceCardPreview}>
                                            {source.contentPreview || '미리보기 내용이 없습니다.'}
                                        </span>
                                        <span className={styles.sourceCardMeta}>
                                            <span>{source.authorNickname || '작성자 없음'}</span>
                                            {typeof source.score === 'number' && (
                                                <span className={styles.sourceCardScore}>
                                                    유사도 {source.score.toFixed(3)}
                                                </span>
                                            )}
                                        </span>
                                    </button>
                                ))}
                            </div>
                        ) : (
                            <p className={styles.loading}>이동할 유사 게시글이 없습니다.</p>
                        )}
                    </div>
                )}

                {aiState.mcpResult && (
                    <div className={styles.aiResult}>
                        <h3 className={styles.resultTitle}>MCP GitHub 결과</h3>
                        <pre className={styles.resultPre}>
                            {JSON.stringify(aiState.mcpResult, null, 2)}
                        </pre>
                    </div>
                )}

                {aiState.agentResult && (
                    <div className={styles.aiResult}>
                        <div className={styles.resultHeader}>
                            <h3 className={styles.resultTitle}>작성 보조 결과</h3>
                            {showAgent && (
                                <Button
                                    type="button"
                                    size="sm"
                                    variant="secondary"
                                    onClick={applyRecommendedTags}
                                >
                                    추천 태그 적용
                                </Button>
                            )}
                        </div>
                        <pre className={styles.resultPre}>
                            {JSON.stringify(aiState.agentResult, null, 2)}
                        </pre>
                    </div>
                )}
            </Card>
        )
    }

    function renderChatBot() {
        return (
            <Card className={styles.chatPanel}>
                <div className={styles.chatHeader}>
                    <h2 className={styles.panelTitle}>지식 Q&A 봇</h2>
                    {chatMessages.length > 0 && (
                        <Button
                            type="button"
                            size="sm"
                            variant="secondary"
                            onClick={() => setChatMessages([])}
                        >
                            <Trash2 size={14} />
                            초기화
                        </Button>
                    )}
                </div>

                <div className={styles.chatMessages}>
                    {chatMessages.length === 0 ? (
                        <p className={styles.chatEmpty}>{isLoggedIn ? '게시판 내용에 대해 질문해 보세요.' : '로그인 후 질문할 수 있습니다.'}</p>
                    ) : (
                        chatMessages.map((message) => (
                            <div
                                className={[
                                    styles.chatMessage,
                                    message.role === 'user' ? styles.chatUserMessage : styles.chatAssistantMessage
                                ].join(' ')}
                                key={message.id}
                            >
                                <div
                                    className={[
                                        styles.chatBubble,
                                        message.role === 'user' ? styles.chatUserBubble : styles.chatAssistantBubble
                                    ].join(' ')}
                                >
                                    <p className={styles.chatContent}>{message.content}</p>

                                    {message.sources?.length > 0 && (
                                        <div className={styles.chatSources}>
                                            <p className={styles.chatSourcesTitle}>참고 게시글</p>
                                            {message.sources.map((source) => (
                                                <button
                                                    className={styles.sourceButton}
                                                    type="button"
                                                    key={source.id}
                                                    onClick={() => loadPost(source.id)}
                                                >
                                                    <span className={styles.sourceButtonTitle}>{source.title}</span>
                                                    <span className={styles.sourceButtonPreview}>{source.contentPreview}</span>
                                                </button>
                                            ))}
                                        </div>
                                    )}

                                    {message.role === 'assistant' && message.originalQuestion && (
                                        <div className={styles.chatActions}>
                                            {message.notionUrl ? (
                                                <a
                                                    className={styles.notionLink}
                                                    href={message.notionUrl}
                                                    target="_blank"
                                                    rel="noreferrer"
                                                >
                                                    <ExternalLink size={14} />
                                                    Notion 열기
                                                </a>
                                            ) : (
                                                <Button
                                                    type="button"
                                                    size="sm"
                                                    variant="secondary"
                                                    disabled={message.notionSaving}
                                                    onClick={() => saveChatAnswerToNotion(message)}
                                                >
                                                    <FilePlus2 size={14} />
                                                    {message.notionSaving ? '저장 중...' : 'Notion에 적기'}
                                                </Button>
                                            )}
                                            {message.notionError && (
                                                <p className={styles.chatActionError}>{message.notionError}</p>
                                            )}
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))
                    )}
                    {chatLoading && <p className={styles.chatEmpty}>답변 생성 중...</p>}
                </div>

                <form className={styles.chatForm} onSubmit={submitChat}>
                    <input
                        className={styles.chatInput}
                        value={chatInput}
                        onChange={(event) => setChatInput(event.target.value)}
                        disabled={!isLoggedIn}
                        placeholder="질문을 입력하세요"
                    />
                    <Button type="submit" disabled={!isLoggedIn || chatLoading || !chatInput.trim()}>
                        <Send size={14} />
                        질문
                    </Button>
                </form>
            </Card>
        )
    }

    if (mode === 'detail') {
        const isOwner = Boolean(user && selectedPost && user.id === selectedPost.authorId)

        return (
            <section className={styles.page}>
                <div className={styles.header}>
                    <h1 className={styles.title}>게시글 상세</h1>
                    <Button type="button" variant="secondary" onClick={backToList}>
                        목록
                    </Button>
                </div>

                {renderChatBot()}

                {selectedPost ? (
                    <>
                        <Card className={styles.detail}>
                            <div className={styles.detailHeader}>
                                <h2 className={styles.detailTitle}>{selectedPost.title}</h2>

                                {isOwner && (
                                    <div className={styles.ownerActions}>
                                        <Button
                                            type="button"
                                            size="sm"
                                            variant="secondary"
                                            onClick={() => startEdit(selectedPost)}
                                        >
                                            수정
                                        </Button>
                                        <Button
                                            type="button"
                                            size="sm"
                                            variant="secondary"
                                            onClick={() => deletePost(selectedPost.id)}
                                        >
                                            삭제
                                        </Button>
                                    </div>
                                )}
                            </div>

                            <div className={styles.meta}>
                                <span>{selectedPost.authorNickname}</span>
                                <time dateTime={selectedPost.createdAt}>
                                    {new Date(selectedPost.createdAt).toLocaleDateString()}
                                </time>
                            </div>
                            <p className={styles.detailContent}>{selectedPost.content}</p>

                            {selectedPost.tags?.length > 0 && (
                                <div className={styles.tags}>
                                    {selectedPost.tags.map((tag) => (
                                        <span className={styles.tag} key={tag}>#{tag}</span>
                                    ))}
                                </div>
                            )}

                            {selectedPost.comments?.length > 0 && (
                                <div className={styles.comments}>
                                    <h3 className={styles.resultTitle}>댓글</h3>
                                    {selectedPost.comments.map((comment) => (
                                        <div className={styles.comment} key={comment.id}>
                                            <div className={styles.commentMeta}>
                                                <span>{comment.authorNickname}</span>
                                                <time dateTime={comment.createdAt}>
                                                    {new Date(comment.createdAt).toLocaleDateString()}
                                                </time>
                                            </div>
                                            <p className={styles.commentContent}>{comment.content}</p>
                                            {user?.id === comment.authorId && (
                                                <Button
                                                    type="button"
                                                    size="sm"
                                                    variant="secondary"
                                                    onClick={() => deleteComment(comment.id)}
                                                >
                                                    댓글 삭제
                                                </Button>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            )}

                            {isLoggedIn ? (
                                <form className={styles.commentForm} onSubmit={handleCommentSubmit}>
                                    <textarea
                                        className={styles.commentTextarea}
                                        value={commentContent}
                                        onChange={(event) => setCommentContent(event.target.value)}
                                        placeholder="댓글을 입력하세요"
                                    />
                                    <Button type="submit">댓글 작성</Button>
                                </form>
                            ) : (
                                <p className={styles.loading}>로그인하면 댓글을 작성할 수 있습니다.</p>
                            )}
                        </Card>

                        {renderAiPanel()}
                        {!aiState.ragResult && !aiState.error && !aiState.loading && (
                            <Button type="button" variant="secondary" onClick={runSimilar}>
                                유사 글 찾기
                            </Button>
                        )}
                    </>
                ) : (
                    <p className={styles.empty}>선택된 게시글이 없습니다.</p>
                )}
            </section>
        )
    }

    if (mode === 'edit') {
        const isEditMode = editingId !== null

        return (
            <section className={styles.page}>
                <div className={styles.header}>
                    <h1 className={styles.title}>{isEditMode ? '게시글 수정' : '게시글 작성'}</h1>
                    <Button type="button" variant="secondary" onClick={backToList}>
                        취소
                    </Button>
                </div>

                {renderChatBot()}

                <Card as="form" className={styles.editor} onSubmit={handleEditorSubmit}>
                    <label className={styles.field}>
                        <span className={styles.labelText}>제목</span>
                        <input
                            className={styles.searchInput}
                            value={editor.title}
                            onChange={(event) => updateEditor('title', event.target.value)}
                            placeholder="제목"
                        />
                    </label>

                    <label className={styles.field}>
                        <span className={styles.labelText}>본문</span>
                        <textarea
                            className={styles.textarea}
                            value={editor.content}
                            onChange={(event) => updateEditor('content', event.target.value)}
                            placeholder="본문"
                        />
                    </label>

                    <label className={styles.field}>
                        <span className={styles.labelText}>태그</span>
                        <input
                            className={styles.searchInput}
                            value={editor.tagsText}
                            onChange={(event) => updateEditor('tagsText', event.target.value)}
                            placeholder="spring, react, api"
                        />
                    </label>

                    <div className={styles.formActions}>
                        <Button type="submit">
                            저장
                        </Button>
                        <Button type="button" variant="secondary" onClick={backToList}>
                            취소
                        </Button>

                    </div>
                </Card>

                {renderAiPanel({ showAgent: true })}
            </section>
        )
    }

    return (
        <section className={styles.page}>
            <header className={styles.header}>
                <h1 className={styles.title}>게시글 목록</h1>
                <Button type="button" disabled={!isLoggedIn} onClick={startCreate}>
                    글쓰기
                </Button>
            </header>

            {renderChatBot()}

            <form className={styles.searchForm} onSubmit={handleSearchSubmit}>
                <input
                    className={styles.searchInput}
                    type="search"
                    placeholder="검색어"
                    value={query.keyword}
                    onChange={(event) => {
                        setQuery((current) => ({ ...current, keyword: event.target.value }))
                    }}
                />
                <input
                    className={styles.searchInput}
                    type="search"
                    placeholder="태그"
                    value={query.tag}
                    onChange={(event) => {
                        setQuery((current) => ({ ...current, tag: event.target.value }))
                    }}
                />
                <Button type="submit">검색</Button>
            </form>

            {loading && <p className={styles.loading}>불러오는 중...</p>}

            <div className={styles.list}>
                {posts.length > 0 ? (
                    posts.map((post) => (
                        <PostListItem
                            key={post.id}
                            post={post}
                            onSelect={() => loadPost(post.id)}
                        />
                    ))
                ) : (
                    <p className={styles.empty}>게시글이 없습니다.</p>
                )}
            </div>

            {pageInfo.totalPages > 1 && (
                <nav className={styles.pagination} aria-label="게시글 페이지">
                    <button
                        className={styles.pageButton}
                        type="button"
                        disabled={pageInfo.page === 0}
                        onClick={() => loadPosts(pageInfo.page - 1)}
                    >
                        이전
                    </button>
                    {pageNumbers.map((pageNumber) => (
                        <button
                            className={[
                                styles.pageButton,
                                pageNumber === pageInfo.page ? styles.activePageButton : ''
                            ].filter(Boolean).join(' ')}
                            key={pageNumber}
                            type="button"
                            onClick={() => loadPosts(pageNumber)}
                            aria-current={pageNumber === pageInfo.page ? 'page' : undefined}
                        >
                            {pageNumber + 1}
                        </button>
                    ))}
                    <button
                        className={styles.pageButton}
                        type="button"
                        disabled={pageInfo.page + 1 >= pageInfo.totalPages}
                        onClick={() => loadPosts(pageInfo.page + 1)}
                    >
                        다음
                    </button>
                </nav>
            )}
        </section>
    )
}
