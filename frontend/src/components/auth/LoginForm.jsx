import { useState } from 'react'
import * as styles from '../../css/LoginForm.css'
import Button from '../common/Button.jsx'
import Card from '../common/Card.jsx'
import { authApi, getErrorMessage } from '../../api/client.js'

export default function LoginForm({onLoginSuccess, setNotice}) {
    const [loginForm, setLoginForm] = useState({
        email: '',
        password: ''
    })

    async function handleLoginSubmit(event) {
        event.preventDefault()

        if (!loginForm.email.trim()) {
            setNotice('이메일을 입력해 주세요.')
            return
        }

        if (!loginForm.password.trim()) {
            setNotice('비밀번호를 입력해 주세요.')
            return
        }

        const payload = {
            email: loginForm.email.trim(),
            password: loginForm.password
        }

        try {
            const authResponse = await authApi.login(payload)
            onLoginSuccess(authResponse)
            setLoginForm({ email: '', password: '' })
        } catch (error) {
            setNotice(getErrorMessage(error))
            console.error(error)
        }
    }

    return (
        <Card as="form" onSubmit={handleLoginSubmit} className={styles.form}>
            <h1 className={styles.title}>로그인</h1>

            <label className={styles.field}>
                <span className={styles.labelText}>이메일</span>
                <input
                    className={styles.input}
                    type="email"
                    placeholder="you@example.com"
                    value={loginForm.email}
                    onChange={(event) =>
                        setLoginForm({
                            ...loginForm,
                            email: event.target.value
                        })
                    }
                />
            </label>

            <label className={styles.field}>
                <span className={styles.labelText}>비밀번호</span>
                <input
                    className={styles.input}
                    type="password"
                    placeholder="비밀번호 입력"
                    value={loginForm.password}
                    onChange={(event) =>
                        setLoginForm({
                            ...loginForm,
                            password: event.target.value
                        })
                    }
                />
            </label>

            <Button size="lg" type="submit">
                로그인
            </Button>
        </Card>
    )
}
