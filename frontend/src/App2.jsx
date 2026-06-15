import { useState } from 'react'
import AppLayout from './components/layout/AppLayout.jsx'
import AuthPage from './components/auth/AuthPage.jsx'
import PostListPage from "./components/post/PostListPage.jsx";
import * as styles from './css/App2.css'

function getSavedUser() {
    const saved = localStorage.getItem('user')

    if (!saved) {
        return null
    }

    try {
        return JSON.parse(saved)
    } catch {
        localStorage.removeItem('user')
        localStorage.removeItem('token')
        return null
    }
}

export default function App2() {
    const [user, setUser] = useState(getSavedUser)
    const [notice, setNotice] = useState('')


    function handleLoginSuccess(authResponse) {
        const loggedInUser = {
            id: authResponse.userId,
            email: authResponse.email,
            nickname: authResponse.nickname
        }

        localStorage.setItem('token', authResponse.token)
        localStorage.setItem('user', JSON.stringify(loggedInUser))
        setUser(loggedInUser)
        setNotice(`${loggedInUser.nickname}님 로그인했습니다.`)
    }

    function handleLogout() {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        setUser(null)
        setNotice('로그아웃했습니다.')
    }

    return (
        <AppLayout user={user} onLogout={handleLogout}>
            {notice && <h2 className={styles.notice}>{notice}</h2>}
            <div className={styles.dashboard}>
                <div className={styles.authColumn}>
                    <AuthPage user={user} onLoginSuccess={handleLoginSuccess} setNotice={setNotice} />
                </div>
                <div className={styles.contentColumn}>
                    <PostListPage user={user} setNotice={setNotice}/>
                </div>
            </div>
        </AppLayout>
    )
}
