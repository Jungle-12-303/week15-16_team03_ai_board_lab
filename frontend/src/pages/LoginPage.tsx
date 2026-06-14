import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'

type LoginPageProps = {
  onLogin: (loginId: string, password: string) => Promise<{ success: boolean; message: string }>
}

function LoginPage({ onLogin }: LoginPageProps) {
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const result = await onLogin(loginId, password)
    setMessage(result.message)

    if (result.success) {
      setLoginId('')
      setPassword('')
    }
  }

  return (
    <div className="auth-page">
      <section className="auth-card">
        <p className="page-kicker">AI Board Lab</p>
        <h1>로그인</h1>
        <p className="auth-description">
          게시글 작성, 댓글 작성, 수정과 삭제는 로그인 후 사용할 수 있습니다.
        </p>

        <form className="auth-form" onSubmit={handleSubmit}>
          <label className="form-label" htmlFor="login-id">
            아이디
          </label>
          <input
            id="login-id"
            className="text-input"
            type="text"
            placeholder="아이디를 입력하세요"
            value={loginId}
            onChange={(event) => setLoginId(event.target.value)}
          />

          <label className="form-label" htmlFor="login-password">
            비밀번호
          </label>
          <input
            id="login-password"
            className="text-input"
            type="password"
            placeholder="비밀번호를 입력하세요"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />

          <button className="primary-button" type="submit">
            로그인
          </button>
        </form>

        {message !== '' && <p className="form-message">{message}</p>}

        <div className="auth-actions">
          <Link className="text-link" to="/signup">
            회원가입으로 이동
          </Link>
          <Link className="text-link" to="/posts">
            게시글 목록 보기
          </Link>
        </div>
      </section>
    </div>
  )
}

export default LoginPage
