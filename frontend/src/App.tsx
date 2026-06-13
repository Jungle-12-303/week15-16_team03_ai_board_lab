import { useEffect, useState } from 'react'

type Post = {
  id: number
  title: string
  content: string
  authorName: string
  createdAt: string
  tags:Tag[]
  ownerLoginId: string
}

type Comment = {
  id:number
  content:string
  authorName:string
  createdAt:string
  ownerLoginId: string
}

type Tag = {
  id:number
  name:string
}

// const API_BASE_URL = 'http://localhost:8080'
// const API_BASE_URL = 'https://your-backend.onrender.com'
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
function App() {
  const [posts, setPosts] = useState<Post[]>([])
  const [selectedPostId, setSelectedPostId] = useState<number | null>(null)
  const [selectedPost, setSelectedPost] = useState<Post | null>(null)
  const [title, setTitle] = useState("")
  const [content, setContent] = useState("")
  const [editingPostId, setEditingPostId] = useState<number | null> (null)
  const [comments, setComments] = useState<Comment[]>([])
  const [commentContent, setCommentContent] = useState('')
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null)
  const [tagInput, setTagInput] = useState("")
  const [tagNames, setTagNames] = useState<string[]>([])
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  const [nickname, setNickname] = useState('')
  const [token, setToken] = useState('')
  const [currentNickname, setCurrentNickname] = useState('')
  const [currentLoginId, setCurrentLoginId] = useState('')

  useEffect(() => {
    fetch(`${API_BASE_URL}/api/posts?keyword=${searchKeyword}&page=${page}&size=10`)
      .then((response) => response.json())
      .then((data) => {
        setPosts(data.content)
        setTotalPages(data.totalPages)
      })
  }, [page, searchKeyword])

  useEffect(() => {
    if(selectedPostId === null){
      return
    }

    fetch(`${API_BASE_URL}/api/posts/${selectedPostId}`)
      .then((response) => response.json())
      .then((data) => setSelectedPost(data))
  }, [selectedPostId])

  useEffect(()=>{
    if(selectedPostId === null){
      return
    } 

   fetch(`${API_BASE_URL}/api/posts/${selectedPostId}/comments`)
    .then((response) => response.json())
    .then((data)=>setComments(data))
  },[selectedPostId])

  
  const handleCreatePost = ()=> {
    if(token === ''){
      return
    }

    fetch(`${API_BASE_URL}/api/posts`,{
      method:'POST',
      headers:{
        'Content-Type':'application/json',
        Authorization:`Bearer ${token}`,
      },
      body: JSON.stringify({
        title: title,
        content: content,
        authorName: currentNickname,
        tagNames: tagNames,
      }),
    })
    .then((response)=>response.json())
    .then((data)=>{
      setPosts([...posts,data])
      setTitle('')
      setContent('')
      setTagInput('')
      setTagNames([])
    })
  }
  
  const handleDeletePost = (id: number) => {
    if (token === '') {
      return
    }

    fetch(`${API_BASE_URL}/api/posts/${id}`, {
      method: 'DELETE',
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }).then(() => {
      setPosts(posts.filter((post) => post.id !== id))

      if (selectedPostId === id) {
        setSelectedPostId(null)
        setSelectedPost(null)
      }
    })
  }

  const handleStartEdit = (post:Post) => {
    setEditingPostId(post.id)
    setTitle(post.title)
    setContent(post.content)
    setTagNames(post.tags.map((tag) => tag.name))
  }

  const handleUpdatePost = ()=> {
    if(editingPostId === null || token ===''){
      return
    }

    fetch(`${API_BASE_URL}/api/posts/${editingPostId}`, {
      method:'PUT',
      headers:{
        'Content-Type': 'application/json',
        Authorization:`Bearer ${token}`,
      },
      body:JSON.stringify({
        title:title,
        content:content,
        authorName: currentNickname,
        tagNames:tagNames,
      }),
    })
      .then((response)=>response.json())
      .then((data)=>{
        setPosts(posts.map((post)=>(post.id === editingPostId ? data:post)))
        setSelectedPost(data)
        setSelectedPostId(data.id)
        setEditingPostId(null)
        setTitle('')
        setContent('')
        setTagInput('')
        setTagNames([])
      })
  }

  const handleCreateComment = () => {
    if (selectedPostId === null || token === "") {
      return
    }

    fetch(`${API_BASE_URL}/api/posts/${selectedPostId}/comments`, {
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
      .then((response) => response.json())
      .then((data) => {
        setComments([...comments, data])
        setCommentContent('')
      })
  }

  const handleDeleteComment = (commentId: number) => {
    if (token === '') {
      return
    }

    fetch(`${API_BASE_URL}/api/comments/${commentId}`, {
      method: 'DELETE',
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })
      .then(() => {
        setComments(comments.filter((comment) => comment.id !== commentId))
      })
  }

  const handleStartEditComment = (comment:Comment)=>{
    setEditingCommentId(comment.id)
    setCommentContent(comment.content)
  }

  const handleUpdateComment = () => {
    if (editingCommentId === null || token === '') {
      return
    }

    fetch(`${API_BASE_URL}/api/comments/${editingCommentId}`, {
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
      .then((response) => response.json())
      .then((data) => {
        setComments(comments.map((comment) => (
          comment.id === editingCommentId ? data : comment
        )))
        setCommentContent('')
        setEditingCommentId(null)
      })
  }

  const handleTagKeyDown = (e:React.KeyboardEvent<HTMLInputElement>)=>{
    if(e.key !== 'Enter'){
      return
    }
    e.preventDefault()
    const trimmedTag = tagInput.trim()

    if(trimmedTag === ''){
      return
    }

    if(tagNames.includes(trimmedTag)){
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
    setPage(0)
    setSearchKeyword(keyword)
  }

  const handleSignUp = () => {
    fetch(`${API_BASE_URL}/api/users/signup`, {
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
      .then((response) => response.json())
      .then(() => {
        setLoginId('')
        setPassword('')
        setNickname('')
      })
  }

  const handleLogin = () => {
    fetch(`${API_BASE_URL}/api/users/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        loginId: loginId,
        password: password,
      }),
    })
      .then((response) => response.json())
      .then((data) => {
        setToken(data.token)
        setCurrentNickname(data.nickname)
        setCurrentLoginId(data.loginId)
        setPassword('')
      })
  }

  return (
    <div>
      <div>
        <h2>회원가입</h2>

        <input
          type="text"
          placeholder="아이디"
          value={loginId}
          onChange={(e) => setLoginId(e.target.value)}
        />
        <br />

        <input
          type="password"
          placeholder="비밀번호"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <br />

        <input
          type="text"
          placeholder="닉네임"
          value={nickname}
          onChange={(e) => setNickname(e.target.value)}
        />
        <br />

        <button onClick={handleSignUp}>회원가입</button>

        <div>
          <h2>로그인</h2>

          <input
            type="text"
            placeholder="아이디"
            value={loginId}
            onChange={(e) => setLoginId(e.target.value)}
          />
          <br />

          <input
            type="password"
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <br />

          <button onClick={handleLogin}>로그인</button>
          <div>
            <p>{token ? '로그인됨' : '로그인 안 됨'}</p>
            {token && <p>현재 사용자: {currentNickname}</p>}
            {token && <p>현재 loginId: {currentLoginId}</p>}
            
          </div>
        </div>
        <br></br>

        <input
          type="text"
          placeholder="검색어 입력"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              handleSearchPosts()
            }
          }}
        />
        <button onClick={handleSearchPosts}>검색</button>
        <h2>게시글 작성</h2>

        <input
          type="text"
          placeholder='제목'
          value={title}
          onChange={(e)=>setTitle(e.target.value)}
        />
        <br></br>
        <textarea
          placeholder='내용'
          value={content}
          onChange={(e)=>setContent(e.target.value)}
        />
        <br></br>

        <input
          type='text'
          placeholder='태그를 입력 후 Enter'
          value={tagInput}
          onChange={(e)=>setTagInput(e.target.value)}
          onKeyDown={handleTagKeyDown}
        />
        <div>
          {tagNames.map((tag) => (
            <span key={tag}>
              {tag}
              <button onClick={() => handleRemoveTag(tag)}>x</button>
            </span>
          ))}
        </div>

         <button onClick={editingPostId === null ? handleCreatePost : handleUpdatePost}>
          {editingPostId === null ? '작성하기' : '수정하기'}
        </button>
      </div>


      <h1>게시글 목록</h1>
      <ul>
        {posts.map((post) => (
          <li key={post.id}>
            <button onClick={() => setSelectedPostId(post.id)}>
              {post.title}
            </button>

            {post.ownerLoginId === currentLoginId && (
              <>
                <button onClick={() => handleStartEdit(post)}>수정</button>
                <button onClick={() => handleDeletePost(post.id)}>삭제</button>
              </>
            )}

            <div>
              {post.tags.map((tag) => tag.name).join(', ')}
            </div>
          </li>
        ))}
      </ul>
      <div>
        <button
          onClick={() => setPage(page - 1)}
          disabled={page === 0}
        >
          이전
        </button>

        <span>
          {page + 1} / {totalPages}
        </span>

        <button
          onClick={() => setPage(page + 1)}
          disabled={page + 1 >= totalPages}
        >
          다음
        </button>
      </div>

      {selectedPost && ( 
        <div>
          <h2>게시글 상세</h2>
          <p>제목: {selectedPost.title}</p>
          <p>내용: {selectedPost.content}</p>
          <p>작성자: {selectedPost.authorName}</p>
          <p>
            태그:
            {selectedPost.tags.map((tag)=>tag.name).join(',')}
          </p>

          
          <h3>댓글</h3>
          <ul>
            {comments.map((comment)=>(
              <li key={comment.id}>
                {comment.authorName}:{comment.content}
                  {comment.ownerLoginId === currentLoginId && (
                    <>
                      <button onClick={() => handleStartEditComment(comment)}>수정</button>
                      <button onClick={() => handleDeleteComment(comment.id)}>삭제</button>
                    </>
                  )}
              </li>
            ))}
          </ul>

          <h3>댓글 작성</h3>
          <br></br>
          <textarea
            placeholder='댓글 내용'
            value={commentContent}
            onChange={(e)=>setCommentContent(e.target.value)}
          />
          <br/>
          <button onClick={editingCommentId === null ? handleCreateComment : handleUpdateComment}>
              {editingCommentId === null ? '댓글 작성' : '댓글 수정'}
          </button>
        </div>
      )}
    </div>
  )
}

export default App