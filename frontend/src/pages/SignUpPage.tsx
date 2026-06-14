import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'

type SignUpPageProps = {
  onSignUp: (
    loginId: string,
    password: string,
    nickname: string,
  ) => Promise<{ success: boolean; message: string }>
}

function SignUpPage({ onSignUp }: SignUpPageProps) {
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  const [nickname, setNickname] = useState('')
  const [message, setMessage] = useState('')

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const result = await onSignUp(loginId, password, nickname)
    setMessage(result.message)

    if (result.success) {
      setLoginId('')
      setPassword('')
      setNickname('')
    }
  }

  return (
    <div className="auth-page">
      <section className="auth-card">
        <p className="page-kicker">AI Board Lab</p>
        <h1>회원가입</h1>
        <p className="auth-description">
          로그인에 사용할 아이디와 게시판에 표시할 닉네임을 등록합니다.
        </p>

        <form className="auth-form" onSubmit={handleSubmit}>
          <label className="form-label" htmlFor="signup-login-id">
            아이디
          </label>
          <input
            id="signup-login-id"
            className="text-input"
            type="text"
            placeholder="아이디를 입력하세요"
            value={loginId}
            onChange={(event) => setLoginId(event.target.value)}
          />

          <label className="form-label" htmlFor="signup-password">
            비밀번호
          </label>
          <input
            id="signup-password"
            className="text-input"
            type="password"
            placeholder="비밀번호를 입력하세요"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />

          <label className="form-label" htmlFor="signup-nickname">
            닉네임
          </label>
          <input
            id="signup-nickname"
            className="text-input"
            type="text"
            placeholder="닉네임을 입력하세요"
            value={nickname}
            onChange={(event) => setNickname(event.target.value)}
          />

          <button className="primary-button" type="submit">
            회원가입
          </button>
        </form>

        {message !== '' && <p className="form-message">{message}</p>}

        <div className="auth-actions">
          <Link className="text-link" to="/login">
            로그인으로 이동
          </Link>
          <Link className="text-link" to="/posts">
            게시글 목록 보기
          </Link>
        </div>
      </section>
    </div>
  )
}

export default SignUpPage
