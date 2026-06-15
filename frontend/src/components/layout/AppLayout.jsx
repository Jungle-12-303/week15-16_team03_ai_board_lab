import TopNav from './TopNav.jsx'
import * as styles from '../../css/AppLayout.css'

export default function AppLayout({ user , onLogout, children }) {
    return (
        <div className={styles.appShell}>
            <TopNav user={user} onLogout={onLogout} />
            <main className={styles.layout}>{children}</main>
        </div>
    )
}
