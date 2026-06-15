import * as styles from '../../css/PostListItem.css'
import Card from '../common/Card.jsx'

export default function PostListItem({ post, onSelect }) {
    return (
        <Card
            as="article"
            className={styles.item}
            onClick={onSelect}
            onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault()
                    onSelect?.()
                }
            }}
            role={onSelect ? 'button' : undefined}
            tabIndex={onSelect ? 0 : undefined}
        >
            <h2 className={styles.title}>{post.title}</h2>

            <p className={styles.preview}>{post.contentPreview}</p>

            <div className={styles.meta}>
                <span>{post.authorNickname}</span>
                <span>{post.commentCount} comments</span>
                <time dateTime={post.createdAt}>
                    {new Date(post.createdAt).toLocaleDateString()}
                </time>
            </div>

            {post.tags?.length > 0 && (
                <div className={styles.tags}>
                    {post.tags.map((tag) => (
                        <span className={styles.tag} key={tag}>#{tag}</span>
                    ))}
                </div>
            )}
        </Card>
    )
}
