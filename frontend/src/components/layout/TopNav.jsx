import { BookOpenText } from 'lucide-react'
import * as styles from '../../css/TopNav.css'
import Button from '../common/Button.jsx'

export default function TopNav({user, onLogout}) {
    return (
        <header className={styles.topbar}>
            <div className={styles.brand}>
                <p className={styles.eyebrow}>AI Knowledge Board</p>
                <h1 className={styles.title}>AI 지식 게시판</h1>
            </div>

            <div className={styles.actions} aria-label="현재 학습 단계">
        <span className={styles.userChip}>
          <BookOpenText size={16} aria-hidden="true" />
          <span>App2 재구현</span>
            {user ? (
                <>
                    <span>{user.nickname}님</span>
                    <Button onClick={onLogout} size="sm" variant="subtle">
                        로그아웃
                    </Button>
                </>
            ) : (
                <span>로그인 전</span>
            )}
        </span>
            </div>
        </header>
    )
}
