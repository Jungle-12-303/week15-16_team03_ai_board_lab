import { useEffect, useState } from 'react'
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import './App.css'
import BoardPage from './pages/BoardPage'
import LoginPage from './pages/LoginPage'
import SignUpPage from './pages/SignUpPage'
import type {
  Comment,
  McpWeatherDraftResponse,
  Post,
  RagAnswerResponse,
  RagReindexResponse,
  RagStatusResponse,
} from './types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const AUTH_STORAGE_KEY = 'ai-board-auth'

type StoredAuth = {
  token: string
  currentNickname: string
  currentLoginId: string
}

type PostPageResponse = {
  content: Post[]
  totalPages: number
}

function getStoredAuth(): StoredAuth {
  if (typeof window === 'undefined') {
    return {
      token: '',
      currentNickname: '',
      currentLoginId: '',
    }
  }

  const rawValue = window.localStorage.getItem(AUTH_STORAGE_KEY)

  if (rawValue === null) {
    return {
      token: '',
      currentNickname: '',
      currentLoginId: '',
    }
  }

  try {
    const parsed = JSON.parse(rawValue) as Partial<StoredAuth>

    return {
      token: parsed.token ?? '',
      currentNickname: parsed.currentNickname ?? '',
      currentLoginId: parsed.currentLoginId ?? '',
    }
  } catch {
    return {
      token: '',
      currentNickname: '',
      currentLoginId: '',
    }
  }
}

function App() {
  const navigate = useNavigate()
  const [posts, setPosts] = useState<Post[]>([])
  const [selectedPostId, setSelectedPostId] = useState<number | null>(null)
  const [selectedPost, setSelectedPost] = useState<Post | null>(null)
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [editingPostId, setEditingPostId] = useState<number | null>(null)
  const [isPostEditorOpen, setIsPostEditorOpen] = useState(false)
  const [comments, setComments] = useState<Comment[]>([])
  const [commentContent, setCommentContent] = useState('')
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null)
  const [tagInput, setTagInput] = useState('')
  const [tagNames, setTagNames] = useState<string[]>([])
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [auth, setAuth] = useState<StoredAuth>(() => getStoredAuth())
  const [ragQuestion, setRagQuestion] = useState('')
  const [ragResult, setRagResult] = useState<RagAnswerResponse | null>(null)
  const [ragError, setRagError] = useState('')
  const [isRagLoading, setIsRagLoading] = useState(false)
  const [isRagReindexing, setIsRagReindexing] = useState(false)
  const [ragStatusMessage, setRagStatusMessage] = useState('')
  const [ragSystemStatus, setRagSystemStatus] = useState<RagStatusResponse | null>(null)
  const [mcpCity, setMcpCity] = useState('Seoul')
  const [mcpForecastDays, setMcpForecastDays] = useState('2')
  const [mcpResult, setMcpResult] = useState<McpWeatherDraftResponse | null>(null)
  const [mcpError, setMcpError] = useState('')
  const [isMcpLoading, setIsMcpLoading] = useState(false)

  const token = auth.token
  const currentNickname = auth.currentNickname
  const currentLoginId = auth.currentLoginId
  const isLoggedIn = token !== ''

  const clearSelectedPost = () => {
    setSelectedPostId(null)
    setSelectedPost(null)
    setComments([])
    setCommentContent('')
    setEditingCommentId(null)
  }

  const resetPostEditor = () => {
    setTitle('')
    setContent('')
    setTagInput('')
    setTagNames([])
    setEditingPostId(null)
  }

  const closePostEditor = () => {
    resetPostEditor()
    setIsPostEditorOpen(false)
  }

  const openCreatePostEditor = () => {
    clearSelectedPost()
    resetPostEditor()
    setIsPostEditorOpen(true)
  }

  const handleUseMcpDraft = () => {
    if (mcpResult === null) {
      return
    }

    clearSelectedPost()
    setEditingPostId(null)
    setTitle(mcpResult.title)
    setContent(mcpResult.content)
    setTagNames(mcpResult.tags)
    setTagInput('')
    setIsPostEditorOpen(true)
  }

  const loadPosts = async (targetPage = page, targetKeyword = searchKeyword) => {
    const response = await fetch(
      `${API_BASE_URL}/api/posts?keyword=${encodeURIComponent(targetKeyword)}&page=${targetPage}&size=10`,
    )
    const data = (await response.json()) as PostPageResponse
    setPosts(data.content)
    setTotalPages(data.totalPages)
  }

  const loadSelectedPost = async (postId: number) => {
    const response = await fetch(`${API_BASE_URL}/api/posts/${postId}`)
    const data = (await response.json()) as Post
    setSelectedPost(data)
  }

  const loadComments = async (postId: number) => {
    const response = await fetch(`${API_BASE_URL}/api/posts/${postId}/comments`)
    const data = (await response.json()) as Comment[]
    setComments(data)
  }

  const loadRagStatus = async () => {
    if (token === '') {
      setRagSystemStatus(null)
      return
    }

    const response = await fetch(`${API_BASE_URL}/api/rag/status`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })

    if (!response.ok) {
      setRagSystemStatus(null)
      return
    }

    const data = (await response.json()) as RagStatusResponse
    setRagSystemStatus(data)
  }

  useEffect(() => {
    if (auth.token === '') {
      window.localStorage.removeItem(AUTH_STORAGE_KEY)
      return
    }

    window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth))
  }, [auth])

  useEffect(() => {
    void loadPosts(page, searchKeyword)
  }, [page, searchKeyword])

  useEffect(() => {
    if (selectedPostId === null) {
      setSelectedPost(null)
      setComments([])
      return
    }

    void loadSelectedPost(selectedPostId)
    void loadComments(selectedPostId)
  }, [selectedPostId])

  useEffect(() => {
    if (token === '') {
      setRagSystemStatus(null)
      return
    }

    void loadRagStatus()
  }, [token])

  const handleCreatePost = async () => {
    if (token === '') {
      return
    }

    const response = await fetch(`${API_BASE_URL}/api/posts`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        title: title,
        content: content,
        authorName: currentNickname,
        tagNames: tagNames,
      }),
    })

    if (!response.ok) {
      return
    }

    const data = (await response.json()) as Post

    if (page === 0) {
      await loadPosts(0, searchKeyword)
    } else {
      setPage(0)
    }

    setSelectedPostId(data.id)
    resetPostEditor()
    setIsPostEditorOpen(false)
    void loadRagStatus()
  }

  const handleDeletePost = async (id: number) => {
    if (token === '') {
      return
    }

    const response = await fetch(`${API_BASE_URL}/api/posts/${id}`, {
      method: 'DELETE',
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })

    if (!response.ok) {
      return
    }

    if (selectedPostId === id) {
      clearSelectedPost()
    }

    if (posts.length === 1 && page > 0) {
      setPage(page - 1)
      void loadRagStatus()
      return
    }

    await loadPosts(page, searchKeyword)
    void loadRagStatus()
  }

  const handleStartEditPost = (post: Post) => {
    clearSelectedPost()
    setEditingPostId(post.id)
    setTitle(post.title)
    setContent(post.content)
    setTagNames(post.tags.map((tag) => tag.name))
    setTagInput('')
    setIsPostEditorOpen(true)
  }

  const handleUpdatePost = async () => {
    if (editingPostId === null || token === '') {
      return
    }

    const response = await fetch(`${API_BASE_URL}/api/posts/${editingPostId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        title: title,
        content: content,
        authorName: currentNickname,
        tagNames: tagNames,
      }),
    })

    if (!response.ok) {
      return
    }

    const data = (await response.json()) as Post

    setPosts(posts.map((post) => (post.id === editingPostId ? data : post)))
    setSelectedPost(data)
    setSelectedPostId(data.id)
    resetPostEditor()
    setIsPostEditorOpen(false)
    void loadRagStatus()
  }

  const handleCreateComment = async () => {
    if (selectedPostId === null || token === '') {
      return
    }

    const response = await fetch(`${API_BASE_URL}/api/posts/${selectedPostId}/comments`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        content: commentContent,
        authorName: currentNickname,
      }),
    })

    if (!response.ok) {
      return
    }

    const data = (await response.json()) as Comment
    setComments([...comments, data])
    setCommentContent('')
  }

  const handleDeleteComment = async (commentId: number) => {
    if (token === '') {
      return
    }

    const response = await fetch(`${API_BASE_URL}/api/comments/${commentId}`, {
      method: 'DELETE',
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })

    if (!response.ok) {
      return
    }

    setComments(comments.filter((comment) => comment.id !== commentId))
  }

  const handleStartEditComment = (comment: Comment) => {
    setEditingCommentId(comment.id)
    setCommentContent(comment.content)
  }

  const handleUpdateComment = async () => {
    if (editingCommentId === null || token === '') {
      return
    }

    const response = await fetch(`${API_BASE_URL}/api/comments/${editingCommentId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        content: commentContent,
        authorName: currentNickname,
      }),
    })

    if (!response.ok) {
      return
    }

    const data = (await response.json()) as Comment
    setComments(comments.map((comment) => (comment.id === editingCommentId ? data : comment)))
    setCommentContent('')
    setEditingCommentId(null)
  }

  const handleTagKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key !== 'Enter') {
      return
    }

    event.preventDefault()
    const trimmedTag = tagInput.trim()

    if (trimmedTag === '') {
      return
    }

    if (tagNames.includes(trimmedTag)) {
      setTagInput('')
      return
    }

    setTagNames([...tagNames, trimmedTag])
    setTagInput('')
  }

  const handleRemoveTag = (tagToRemove: string) => {
    setTagNames(tagNames.filter((tag) => tag !== tagToRemove))
  }

  const handleSearchPosts = () => {
    clearSelectedPost()
    setPage(0)
    setSearchKeyword(keyword)
  }

  const handleSignUp = async (loginId: string, password: string, nickname: string) => {
    const response = await fetch(`${API_BASE_URL}/api/users/signup`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        loginId: loginId,
        password: password,
        nickname: nickname,
      }),
    })

    if (!response.ok) {
      return {
        success: false,
        message: '회원가입에 실패했습니다. 입력값을 다시 확인하세요.',
      }
    }

    navigate('/login')

    return {
      success: true,
      message: '회원가입이 완료되었습니다. 이제 로그인하세요.',
    }
  }

  const handleLogin = async (loginId: string, password: string) => {
    const response = await fetch(`${API_BASE_URL}/api/users/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        loginId: loginId,
        password: password,
      }),
    })

    if (!response.ok) {
      return {
        success: false,
        message: '로그인에 실패했습니다. 아이디와 비밀번호를 확인하세요.',
      }
    }

    const data = (await response.json()) as {
      token: string
      nickname: string
      loginId: string
    }

    setAuth({
      token: data.token,
      currentNickname: data.nickname,
      currentLoginId: data.loginId,
    })
    navigate('/posts')

    return {
      success: true,
      message: '로그인되었습니다.',
    }
  }

  const handleLogout = () => {
    setAuth({
      token: '',
      currentNickname: '',
      currentLoginId: '',
    })
    closePostEditor()
    clearSelectedPost()
    setRagQuestion('')
    setRagResult(null)
    setRagError('')
    setRagStatusMessage('')
    setRagSystemStatus(null)
    setMcpResult(null)
    setMcpError('')
    navigate('/login')
  }

  const readErrorMessage = async (response: Response) => {
    try {
      const errorData = (await response.json()) as { message?: string }
      return errorData.message ?? '요청 처리 중 오류가 발생했습니다.'
    } catch {
      return '요청 처리 중 오류가 발생했습니다.'
    }
  }

  const handleAskRag = async () => {
    if (token === '') {
      setRagError('AI 검색은 로그인 후 사용할 수 있습니다.')
      return
    }

    const trimmedQuestion = ragQuestion.trim()

    if (trimmedQuestion === '') {
      setRagError('질문을 입력해주세요.')
      return
    }

    setIsRagLoading(true)
    setRagError('')
    setRagStatusMessage('')

    try {
      const response = await fetch(`${API_BASE_URL}/api/rag/ask`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          question: trimmedQuestion,
        }),
      })

      if (!response.ok) {
        setRagResult(null)
        setRagError(await readErrorMessage(response))
        return
      }

      const data = (await response.json()) as RagAnswerResponse
      setRagResult(data)
    } finally {
      setIsRagLoading(false)
    }
  }

  const handleReindexPosts = async () => {
    if (token === '') {
      setRagError('재임베딩은 로그인 후 사용할 수 있습니다.')
      return
    }

    setIsRagReindexing(true)
    setRagError('')
    setRagStatusMessage('')

    try {
      const response = await fetch(`${API_BASE_URL}/api/rag/reindex/posts`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      if (!response.ok) {
        setRagStatusMessage('')
        setRagError(await readErrorMessage(response))
        return
      }

      const data = (await response.json()) as RagReindexResponse
      setRagStatusMessage(
        `재임베딩 완료: 전체 ${data.totalPosts}개 중 ${data.successCount}개 성공, ${data.failedCount}개 실패`,
      )
      await loadRagStatus()
    } finally {
      setIsRagReindexing(false)
    }
  }

  const handleAskMcpWeatherDraft = async () => {
    if (token === '') {
      setMcpError('MCP 기능은 로그인 후 사용할 수 있습니다.')
      return
    }

    const trimmedCity = mcpCity.trim()

    if (trimmedCity === '') {
      setMcpError('도시 이름을 입력해주세요.')
      return
    }

    const parsedForecastDays = Number(mcpForecastDays)

    if (Number.isNaN(parsedForecastDays) || parsedForecastDays < 1 || parsedForecastDays > 3) {
      setMcpError('예보 일수는 1에서 3 사이로 입력해주세요.')
      return
    }

    setIsMcpLoading(true)
    setMcpError('')

    try {
      const response = await fetch(`${API_BASE_URL}/api/mcp/weather-draft`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          city: trimmedCity,
          forecastDays: parsedForecastDays,
        }),
      })

      if (!response.ok) {
        setMcpResult(null)
        setMcpError(await readErrorMessage(response))
        return
      }

      const data = (await response.json()) as McpWeatherDraftResponse
      setMcpResult(data)
    } finally {
      setIsMcpLoading(false)
    }
  }

  return (
    <Routes>
      <Route path="/" element={<Navigate replace to="/posts" />} />
      <Route
        path="/login"
        element={
          isLoggedIn ? (
            <Navigate replace to="/posts" />
          ) : (
            <LoginPage onLogin={handleLogin} />
          )
        }
      />
      <Route
        path="/signup"
        element={
          isLoggedIn ? (
            <Navigate replace to="/posts" />
          ) : (
            <SignUpPage onSignUp={handleSignUp} />
          )
        }
      />
      <Route
        path="/posts"
        element={
          <BoardPage
            comments={comments}
            commentContent={commentContent}
            content={content}
            currentLoginId={currentLoginId}
            currentNickname={currentNickname}
            editingCommentId={editingCommentId}
            editingPostId={editingPostId}
            isPostEditorOpen={isPostEditorOpen}
            isLoggedIn={isLoggedIn}
            keyword={keyword}
            onCommentContentChange={setCommentContent}
            onContentChange={setContent}
            onBackToList={clearSelectedPost}
            onClosePostEditor={closePostEditor}
            onCreateComment={handleCreateComment}
            onCreatePost={handleCreatePost}
            onDeleteComment={handleDeleteComment}
            onDeletePost={handleDeletePost}
            onLogout={handleLogout}
            onOpenCreatePost={openCreatePostEditor}
            onPageChange={(nextPage) => {
              clearSelectedPost()
              setPage(nextPage)
            }}
            onAskMcpWeatherDraft={handleAskMcpWeatherDraft}
            onAskRag={handleAskRag}
            onReindexPosts={handleReindexPosts}
            onMcpCityChange={setMcpCity}
            onMcpForecastDaysChange={setMcpForecastDays}
            onRagQuestionChange={setRagQuestion}
            onRemoveTag={handleRemoveTag}
            onSearch={handleSearchPosts}
            onSearchKeywordChange={setKeyword}
            onSelectPost={(id) => {
              setCommentContent('')
              setEditingCommentId(null)
              setSelectedPostId(id)
            }}
            onStartEditComment={handleStartEditComment}
            onStartEditPost={handleStartEditPost}
            onTagInputChange={setTagInput}
            onTagKeyDown={handleTagKeyDown}
            onTitleChange={setTitle}
            onUpdateComment={handleUpdateComment}
            onUpdatePost={handleUpdatePost}
            onUseMcpDraft={handleUseMcpDraft}
            isMcpLoading={isMcpLoading}
            page={page}
            posts={posts}
            mcpCity={mcpCity}
            mcpError={mcpError}
            mcpForecastDays={mcpForecastDays}
            mcpResult={mcpResult}
            ragError={ragError}
            ragQuestion={ragQuestion}
            ragResult={ragResult}
            isRagLoading={isRagLoading}
            isRagReindexing={isRagReindexing}
            ragStatusMessage={ragStatusMessage}
            ragSystemStatus={ragSystemStatus}
            selectedPost={selectedPost}
            selectedPostId={selectedPostId}
            tagInput={tagInput}
            tagNames={tagNames}
            title={title}
            totalPages={totalPages}
          />
        }
      />
    </Routes>
  )
}

export default App
