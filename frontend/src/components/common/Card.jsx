import * as styles from './Card.css'

export default function Card({ as: Component = 'section', children, className = '', ...props }) {
    return (
        <Component className={[styles.card, className].filter(Boolean).join(' ')} {...props}>
            {children}
        </Component>
    )
}
