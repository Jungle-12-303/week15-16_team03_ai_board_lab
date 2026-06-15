import { style } from '@vanilla-extract/css'
import { colors } from '../styles/theme.css'

export const appShell = style({
    minHeight: '100vh',
    backgroundColor: colors.background,
    color: colors.text
})

export const layout = style({
    width: '100%',
    maxWidth: '1180px',
    margin: '0 auto',
    padding: '28px',
    '@media': {
        'screen and (max-width: 640px)': {
            padding: '20px'
        }
    }
})
