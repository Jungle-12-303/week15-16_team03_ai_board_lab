import { useEffect, useState } from 'react'

type Post = {
  id: number
  title: string
  content: string
  authorName: string
  createdAt: string
}

function App() {
  const [posts, setPosts] = useState<Post[]>([])
  const [selectedPostId, setSelectedPostId] = useState<number | null>(null)
  const [selectedPost, setSelectedPost] = useState<Post | null>(null)
  
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

  return (
    <div>
      <h1>게시글 목록</h1>
      <ul>
        {posts.map((post) => (  
          <li key={post.id}>
            <button onClick={()=> setSelectedPostId(post.id)}>
              {post.title}
            </button>            
          </li>
        ))}
      </ul>

      {selectedPost && (
        <div>
          <h2>게시글 상세</h2>
          <p>제목: {selectedPost.title}</p>
          <p>내용: {selectedPost.content}</p>
          <p>작성자: {selectedPost.authorName}</p>
        </div>
      )}
    </div>
  )
}

export default App