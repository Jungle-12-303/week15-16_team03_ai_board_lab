import { useEffect, useState } from 'react'

type Post = {
  id: number
  title: string
  content: string
  authorName: string
  createdAt: string
  tags:Tag[]
}

type Comment = {
  id:number
  content:string
  authorName:string
  createdAt:string
}

type Tag = {
  id:number
  name:string
}

function App() {
  const [posts, setPosts] = useState<Post[]>([])
  const [selectedPostId, setSelectedPostId] = useState<number | null>(null)
  const [selectedPost, setSelectedPost] = useState<Post | null>(null)
  const [title, setTitle] = useState("")
  const [content, setContent] = useState("")
  const [authorName, setAuthorName] = useState("")
  const [editingPostId, setEditingPostId] = useState<number | null> (null)
  const [comments, setComments] = useState<Comment[]>([])
  const [commentContent, setCommentContent] = useState('')
  const [commentAuthorName, setCommentAuthorName] = useState('')
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null)
  const [tagInput, setTagInput] = useState("")
  const [tagNames, setTagNames] = useState<string[]>([])

  useEffect(() => {
  fetch(`http://localhost:8080/api/posts`)
    .then((response) => response.json())
    .then((data) => setPosts(data))
  }, [])

  useEffect(() => {
    if(selectedPostId === null){
      return
    }

    fetch(`http://localhost:8080/api/posts/${selectedPostId}`)
      .then((response) => response.json())
      .then((data) => setSelectedPost(data))
  }, [selectedPostId])

  useEffect(()=>{
    if(selectedPostId === null){
      return
    }

   fetch(`http://localhost:8080/api/posts/${selectedPostId}/comments`)
    .then((response) => response.json())
    .then((data)=>setComments(data))
  },[selectedPostId])

  
  const handleCreatePost = ()=> {
    fetch('http://localhost:8080/api/posts',{
      method:'POST',
      headers:{
        'Content-Type':'application/json',
      },
      body:JSON.stringify({
        title:title,
        content:content,
        authorName:authorName,
        tagNames: tagNames,
      }),
    })
    .then((response)=>response.json())
    .then((data)=>{
      setPosts([...posts,data])
      setTitle('')
      setContent('')
      setAuthorName('')
      setTagInput('')
      setTagNames([])
    })
  }
  
  const handleDeletePost = (id:number)=>{
    fetch(`http://localhost:8080/api/posts/${id}` ,{
      method:'DELETE',
    }).then(()=>{
      setPosts(posts.filter((post) => post.id !== id))

      if(selectedPostId === id){
        setSelectedPostId(null)
        setSelectedPost(null)
      }
    })
  }

  const handleStartEdit = (post:Post) => {
    setEditingPostId(post.id)
    setTitle(post.title)
    setContent(post.content)
    setAuthorName(post.authorName)
    setTagNames(post.tags.map((tag) => tag.name))
  }

  const handleUpdatePost = ()=> {
    if(editingPostId === null){
      return
    }

    fetch(`http://localhost:8080/api/posts/${editingPostId}`, {
      method:'PUT',
      headers:{
        'Content-Type': 'application/json',
      },
      body:JSON.stringify({
        title:title,
        content:content,
        authorName:authorName,
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
        setAuthorName('')
        setTagInput('')
        setTagNames([])
      })
  }

  const handleCreateComment = ()=>{
    if(selectedPostId === null){
      return
    }
    fetch(`http://localhost:8080/api/posts/${selectedPostId}/comments`, {
      method:'POST',
      headers:{
        'Content-Type':'application/json',
      },
      body:JSON.stringify({
        content:commentContent,
        authorName:commentAuthorName,
      }),
    })
    .then((Response)=>Response.json())
    .then((data)=>{
      setComments([...comments,data])
      setCommentContent('')
      setCommentAuthorName('')
    })
  }

  const handleDeleteComment = (commentId:number)=>{
    fetch(`http://localhost:8080/api/comments/${commentId}`,{
      method:'DELETE',
    }).then(()=>{
      setComments(comments.filter((comment)=>comment.id !== commentId))
    })
  }

  const handleStartEditComment = (comment:Comment)=>{
    setEditingCommentId(comment.id)
    setCommentContent(comment.content)
  }

  const handleUpdateComment = ()=>{
    if(editingCommentId === null){
      return
    }

    fetch(`http://localhost:8080/api/comments/${editingCommentId}`, {
      method:'PUT',
      headers:{
        'Content-Type': 'application/json',
      },
      body:JSON.stringify({
        content:commentContent,
        authorName:commentAuthorName,
      }),
    })
    .then((response)=>response.json())
    .then((data)=>{
      setComments(comments.map((comment) => (
        comment.id === editingCommentId ? data : comment
      )))
      setEditingCommentId(null)
      setCommentContent('')
      setCommentAuthorName('')
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

  return (
    <div>
      <div>
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
          placeholder='작성자'
          value={authorName}
          onChange={(e)=>setAuthorName(e.target.value)}
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
            <button onClick={() => handleStartEdit(post)}>수정</button>
            <button onClick={() => handleDeletePost(post.id)}>삭제</button>
            <div>
              {post.tags.map((tag) => tag.name).join(', ')}
            </div>
          </li>
        ))}
      </ul>

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
                <button onClick={()=> handleStartEditComment(comment)}>수정</button>
                <button onClick={()=> handleDeleteComment(comment.id)}>삭제</button>
              </li>
            ))}
          </ul>

          <h3>댓글 작성</h3>
          <input
            type='text'
            placeholder='작성자'
            value={commentAuthorName}
            onChange={(e)=>setCommentAuthorName(e.target.value)}
          />
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