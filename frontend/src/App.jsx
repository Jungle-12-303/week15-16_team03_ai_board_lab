import { useEffect, useMemo, useState } from 'react'
import {
  Bot,
  Edit3,
  LogIn,
  LogOut,
  MessageCircle,
  Plus,
  RefreshCw,
  Save,
  Search,
  Sparkles,
  Trash2
} from 'lucide-react'
import { aiApi, authApi, getErrorMessage, postApi } from './api/client.js'
import './App.css'

const emptyEditor = { title: '', content: '', tags: '' }

export default function App() {
  const [user, setUser] = useState(() => JSON.parse(localStorage.getItem('user') || 'null'))
  const [posts, setPosts] = useState([])
  const [pageInfo, setPageInfo] = useState({ page: 0, totalPages: 0 })
  const [query, setQuery] = useState({ keyword: '', tag: '' })
  const [selectedPost, setSelectedPost] = useState(null)
  const [mode, setMode] = useState('list')
  const [editingId, setEditingId] = useState(null)
  const [editor, setEditor] = useState(emptyEditor)
  const [comment, setComment] = useState('')
  const [aiState, setAiState] = useState({ loading: false, similar: null, agent: null, mcp: null })
  const [notice, setNotice] = useState('')
  const [loading, setLoading] = useState(false)

  const isLoggedIn = Boolean(user)

  useEffect(() => {
    loadPosts(0)
  }, [])

  async function loadPosts(page = pageInfo.page) {
    setLoading(true)
    setNotice('')
    try {
      const data = await postApi.list({
        page,
        size: 8,
        keyword: query.keyword || undefined,
        tag: query.tag || undefined
      })
      setPosts(data.content)
      setPageInfo({ page: data.page, totalPages: data.totalPages })
    } catch (error) {
      setNotice(getErrorMessage(error))
    } finally {
      setLoading(false)
    }
  }

  async function loadPost(id) {
    setLoading(true)
    setNotice('')
    try {
      const data = await postApi.get(id)
      setSelectedPost(data)
      setMode('detail')
      setAiState({ loading: false, similar: null, agent: null, mcp: null })
    } catch (error) {
      setNotice(getErrorMessage(error))
    } finally {
      setLoading(false)
    }
  }

  function startCreate() {
    setNotice('')
    setEditingId(null)
    setEditor(emptyEditor)
    setSelectedPost(null)
    setAiState({ loading: false, similar: null, agent: null, mcp: null })
    setMode('edit')
  }

  function startEdit(post) {
    setNotice('')
    setEditingId(post.id)
    setEditor({
      title: post.title,
      content: post.content,
      tags: post.tags.join(', ')
    })
    setAiState({ loading: false, similar: null, agent: null, mcp: null })
    setMode('edit')
  }

  async function savePost(event) {
    event.preventDefault()
    if (!isLoggedIn) {
      setNotice('로그인 후 게시글을 작성할 수 있습니다.')
      return
    }
    const payload = {
      title: editor.title,
      content: editor.content,
      tags: editor.tags.split(',').map((tag) => tag.trim()).filter(Boolean)
    }
    try {
      const saved = editingId ? await postApi.update(editingId, payload) : await postApi.create(payload)
      setNotice(editingId ? '게시글을 수정했습니다.' : '게시글을 작성했습니다.')
      await loadPosts(0)
      await loadPost(saved.id)
    } catch (error) {
      setNotice(getErrorMessage(error))
    }
  }

  async function deletePost(id) {
    if (!confirm('게시글을 삭제할까요?')) return
    try {
      await postApi.remove(id)
      setSelectedPost(null)
      setMode('list')
      await loadPosts(0)
      setNotice('게시글을 삭제했습니다.')
    } catch (error) {
      setNotice(getErrorMessage(error))
    }
  }

  async function saveComment(event) {
    event.preventDefault()
    if (!comment.trim() || !selectedPost) return
    try {
      await postApi.comment(selectedPost.id, { content: comment })
      setComment('')
      await loadPost(selectedPost.id)
    } catch (error) {
      setNotice(getErrorMessage(error))
    }
  }

  async function deleteComment(commentId) {
    try {
      await postApi.removeComment(commentId)
      await loadPost(selectedPost.id)
    } catch (error) {
      setNotice(getErrorMessage(error))
    }
  }

  async function runSimilar() {
    const draft = mode === 'detail' && selectedPost
      ? `${selectedPost.title}\n${selectedPost.content}`
      : `${editor.title}\n${editor.content}`
    setAiState((state) => ({ ...state, loading: true }))
    try {
      const data = await aiApi.similar({ query: draft, excludePostId: selectedPost?.id })
      setAiState((state) => ({ ...state, similar: data }))
    } catch (error) {
      setNotice(getErrorMessage(error))
    } finally {
      setAiState((state) => ({ ...state, loading: false }))
    }
  }

  async function runAgent() {
    setAiState((state) => ({ ...state, loading: true }))
    try {
      const data = await aiApi.agent({ draft: `${editor.title}\n${editor.content}`, intention: '게시글 작성 보조' })
      setAiState((state) => ({ ...state, agent: data }))
      if (data.recommendedTags?.length) {
        setEditor((current) => ({
          ...current,
          tags: mergeTags(current.tags, data.recommendedTags)
        }))
      }
    } catch (error) {
      setNotice(getErrorMessage(error))
    } finally {
      setAiState((state) => ({ ...state, loading: false }))
    }
  }

  async function runMcp(username = 'openai') {
    setAiState((state) => ({ ...state, loading: true }))
    try {
      const data = await aiApi.mcp({
        jsonrpc: '2.0',
        method: 'github.getUser',
        params: { username },
        id: `ui-${Date.now()}`
      })
      setAiState((state) => ({ ...state, mcp: data }))
    } catch (error) {
      setNotice(getErrorMessage(error))
    } finally {
      setAiState((state) => ({ ...state, loading: false }))
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">React + Spring Boot + RAG + MCP + Agent</p>
          <h1>AI 지식 게시판</h1>
        </div>
        <div className="topbar-actions">
          {user ? (
            <>
              <span className="user-chip">{user.nickname}</span>
              <button className="ghost-button" onClick={() => logout(setUser)}>
                <LogOut size={16} /> 로그아웃
              </button>
            </>
          ) : null}
        </div>
      </header>

      <main className="layout">
        <aside className="sidebar">
          <AuthPanel user={user} setUser={setUser} setNotice={setNotice} />
          <section className="panel">
            <h2>검색</h2>
            <form
              className="stack"
              onSubmit={(event) => {
                event.preventDefault()
                loadPosts(0)
              }}
            >
              <input value={query.keyword} onChange={(event) => setQuery({ ...query, keyword: event.target.value })} placeholder="제목/본문 검색" />
              <input value={query.tag} onChange={(event) => setQuery({ ...query, tag: event.target.value })} placeholder="태그 필터" />
              <button type="submit">
                <Search size={16} /> 검색
              </button>
            </form>
          </section>
        </aside>

        <section className="workspace">
          <div className="toolbar">
            <button onClick={() => { setMode('list'); setSelectedPost(null); loadPosts(pageInfo.page) }}>
              <RefreshCw size={16} /> 목록
            </button>
            <button onClick={startCreate}>
              <Plus size={16} /> 글쓰기
            </button>
            {loading ? <span className="muted">불러오는 중...</span> : null}
          </div>

          {notice ? <div className="notice">{notice}</div> : null}

          {mode === 'list' ? (
            <PostList posts={posts} pageInfo={pageInfo} onSelect={loadPost} onPage={loadPosts} />
          ) : null}

          {mode === 'detail' && selectedPost ? (
            <PostDetail
              post={selectedPost}
              user={user}
              comment={comment}
              setComment={setComment}
              onComment={saveComment}
              onDeleteComment={deleteComment}
              onEdit={() => startEdit(selectedPost)}
              onDelete={() => deletePost(selectedPost.id)}
              onSimilar={runSimilar}
              aiState={aiState}
            />
          ) : null}

          {mode === 'edit' ? (
            <PostEditor
              editor={editor}
              setEditor={setEditor}
              editingId={editingId}
              onSubmit={savePost}
              onAgent={runAgent}
              onSimilar={runSimilar}
              onMcp={runMcp}
              aiState={aiState}
            />
          ) : null}
        </section>
      </main>
    </div>
  )
}

function AuthPanel({ user, setUser, setNotice }) {
  const [mode, setMode] = useState('login')
  const [form, setForm] = useState({ email: 'demo@example.com', nickname: 'demo', password: 'password123' })
  const title = mode === 'login' ? '로그인' : '회원가입'

  async function submit(event) {
    event.preventDefault()
    try {
      const data = mode === 'login'
        ? await authApi.login({ email: form.email, password: form.password })
        : await authApi.signup(form)
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify({ id: data.userId, email: data.email, nickname: data.nickname }))
      setUser({ id: data.userId, email: data.email, nickname: data.nickname })
      setNotice(`${data.nickname} 계정으로 로그인했습니다.`)
    } catch (error) {
      setNotice(getErrorMessage(error))
    }
  }

  if (user) {
    return (
      <section className="panel">
        <h2>내 계정</h2>
        <p className="muted">{user.email}</p>
      </section>
    )
  }

  return (
    <section className="panel">
      <h2>{title}</h2>
      <form className="stack" onSubmit={submit}>
        <input value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} placeholder="email" />
        {mode === 'signup' ? (
          <input value={form.nickname} onChange={(event) => setForm({ ...form, nickname: event.target.value })} placeholder="nickname" />
        ) : null}
        <input type="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} placeholder="password" />
        <button type="submit">
          <LogIn size={16} /> {title}
        </button>
      </form>
      <button className="link-button" onClick={() => setMode(mode === 'login' ? 'signup' : 'login')}>
        {mode === 'login' ? '회원가입으로 전환' : '로그인으로 전환'}
      </button>
    </section>
  )
}

function PostList({ posts, pageInfo, onSelect, onPage }) {
  if (!posts.length) {
    return <div className="empty">게시글이 없습니다. 첫 글을 작성해 보세요.</div>
  }
  return (
    <>
      <div className="post-list">
        {posts.map((post) => (
          <article className="post-row" key={post.id} onClick={() => onSelect(post.id)}>
            <div>
              <h2>{post.title}</h2>
              <p>{post.contentPreview}</p>
              <TagList tags={post.tags} />
            </div>
            <div className="post-meta">
              <span>{post.authorNickname}</span>
              <span><MessageCircle size={14} /> {post.commentCount}</span>
            </div>
          </article>
        ))}
      </div>
      <div className="pagination">
        <button disabled={pageInfo.page <= 0} onClick={() => onPage(pageInfo.page - 1)}>이전</button>
        <span>{pageInfo.page + 1} / {Math.max(pageInfo.totalPages, 1)}</span>
        <button disabled={pageInfo.page + 1 >= pageInfo.totalPages} onClick={() => onPage(pageInfo.page + 1)}>다음</button>
      </div>
    </>
  )
}

function PostDetail({ post, user, comment, setComment, onComment, onDeleteComment, onEdit, onDelete, onSimilar, aiState }) {
  const isOwner = user?.id === post.authorId
  return (
    <article className="detail">
      <div className="detail-header">
        <div>
          <h2>{post.title}</h2>
          <p className="muted">{post.authorNickname}</p>
        </div>
        {isOwner ? (
          <div className="inline-actions">
            <button onClick={onEdit}><Edit3 size={16} /> 수정</button>
            <button className="danger" onClick={onDelete}><Trash2 size={16} /> 삭제</button>
          </div>
        ) : null}
      </div>
      <TagList tags={post.tags} />
      <p className="content-block">{post.content}</p>
      <section className="ai-strip">
        <button onClick={onSimilar} disabled={aiState.loading}>
          <Sparkles size={16} /> 유사 글 찾기
        </button>
        <AiResult aiState={aiState} />
      </section>
      <section className="comments">
        <h3>댓글</h3>
        {post.comments.map((item) => (
          <div className="comment" key={item.id}>
            <div>
              <strong>{item.authorNickname}</strong>
              <p>{item.content}</p>
            </div>
            {user?.id === item.authorId ? (
              <button className="icon-danger" onClick={() => onDeleteComment(item.id)} title="댓글 삭제">
                <Trash2 size={16} />
              </button>
            ) : null}
          </div>
        ))}
        <form className="comment-form" onSubmit={onComment}>
          <input value={comment} onChange={(event) => setComment(event.target.value)} placeholder="댓글 입력" />
          <button type="submit"><MessageCircle size={16} /> 등록</button>
        </form>
      </section>
    </article>
  )
}

function PostEditor({ editor, setEditor, editingId, onSubmit, onAgent, onSimilar, onMcp, aiState }) {
  const [githubUser, setGithubUser] = useState('openai')
  return (
    <div className="editor-grid">
      <form className="editor" onSubmit={onSubmit}>
        <h2>{editingId ? '게시글 수정' : '게시글 작성'}</h2>
        <input value={editor.title} onChange={(event) => setEditor({ ...editor, title: event.target.value })} placeholder="제목" />
        <textarea value={editor.content} onChange={(event) => setEditor({ ...editor, content: event.target.value })} placeholder="본문" rows={14} />
        <input value={editor.tags} onChange={(event) => setEditor({ ...editor, tags: event.target.value })} placeholder="태그, 쉼표로 구분" />
        <button type="submit"><Save size={16} /> 저장</button>
      </form>
      <aside className="ai-panel">
        <h2><Bot size={18} /> AI 작성 도우미</h2>
        <button onClick={onAgent} disabled={aiState.loading}><Sparkles size={16} /> Agent 실행</button>
        <button onClick={onSimilar} disabled={aiState.loading}><Search size={16} /> 유사 글 검색</button>
        <div className="mcp-row">
          <input value={githubUser} onChange={(event) => setGithubUser(event.target.value)} placeholder="GitHub username" />
          <button type="button" onClick={() => onMcp(githubUser)} disabled={aiState.loading}>MCP</button>
        </div>
        <AiResult aiState={aiState} />
      </aside>
    </div>
  )
}

function AiResult({ aiState }) {
  return (
    <div className="ai-result">
      {aiState.loading ? <p className="muted">AI 요청 처리 중...</p> : null}
      {aiState.agent ? (
        <section>
          <h3>Agent 결과</h3>
          <p>{aiState.agent.finalMessage}</p>
          <TagList tags={aiState.agent.recommendedTags || []} />
          <ul>
            {(aiState.agent.steps || []).map((step, index) => (
              <li key={`${step.tool}-${index}`}>{step.tool}: {step.status}</li>
            ))}
          </ul>
        </section>
      ) : null}
      {aiState.similar ? (
        <section>
          <h3>RAG 요약</h3>
          <p>{aiState.similar.summary}</p>
          {(aiState.similar.sources || []).map((source) => (
            <div className="source" key={source.id}>
              <strong>{source.title}</strong>
              <span>{source.link} · score {source.score.toFixed(3)}</span>
            </div>
          ))}
        </section>
      ) : null}
      {aiState.mcp ? (
        <section>
          <h3>MCP 결과</h3>
          <pre>{JSON.stringify(aiState.mcp, null, 2)}</pre>
        </section>
      ) : null}
    </div>
  )
}

function TagList({ tags }) {
  if (!tags?.length) return null
  return (
    <div className="tags">
      {tags.map((tag) => <span key={tag}>#{tag}</span>)}
    </div>
  )
}

function mergeTags(current, recommended) {
  const merged = new Set(current.split(',').map((tag) => tag.trim()).filter(Boolean))
  recommended.forEach((tag) => merged.add(tag))
  return [...merged].join(', ')
}

function logout(setUser) {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  setUser(null)
}
