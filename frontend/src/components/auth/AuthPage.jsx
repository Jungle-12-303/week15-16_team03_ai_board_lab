import { useState } from 'react'
import LoginForm from './LoginForm.jsx'
import SignupForm from './SignUpForm.jsx'
import Button from '../common/Button.jsx'
import Card from '../common/Card.jsx'
import * as styles from '../../css/AuthPage.css'

export default function AuthPage({user, onLoginSuccess, setNotice}) {
    const [authMode, setAuthMode] = useState('login')

    if (user) {
        return (
            <Card className={styles.userCard}>
                <p className={styles.kicker}>로그인 사용자</p>
                <h2 className={styles.userName}>{user.nickname}</h2>
                <p className={styles.userEmail}>{user.email}</p>
            </Card>
        )
    }

    const isLoginMode = authMode === 'login'

    return (
        <section className={styles.authPanel}>
            {isLoginMode ? (
                <LoginForm onLoginSuccess={onLoginSuccess} setNotice={setNotice}/>
            ) : (
                <SignupForm setNotice={setNotice}/>
            )}

            <div className={styles.switchBox}>
                <p className={styles.switchText}>
                    {isLoginMode ? '아직 계정이 없나요?' : '이미 계정이 있나요?'}
                </p>
                <Button
                    size="sm"
                    type="button"
                    variant="subtle"
                    onClick={() => setAuthMode(isLoginMode ? 'signup' : 'login')}
                >
                    {isLoginMode ? '회원가입' : '로그인'}
                </Button>
            </div>
        </section>
    )
}
