import * as styles from './Button.css'

export default function Button({
    children,
    className = '',
    size = 'md',
    type = 'button',
    variant = 'primary',
    ...props
}) {
    return (
        <button
            className={[styles.button, styles.variant[variant], styles.size[size], className].filter(Boolean).join(' ')}
            type={type}
            {...props}
        >
            {children}
        </button>
    )
}
