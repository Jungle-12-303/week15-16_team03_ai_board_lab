import { style } from '@vanilla-extract/css'
import { colors, fontSizes, radii, space } from '../styles/theme.css'

export const form = style({
    width: '100%',
    maxWidth: '280px',
    display: 'flex',
    flexDirection: 'column',
    gap: space.lg
})

export const title = style({
    margin: 0,
    fontSize: fontSizes.xxl,
    fontWeight: 700,
    color: colors.text
})

export const field = style({
    display: 'flex',
    flexDirection: 'column',
    gap: '6px'
})

export const labelText = style({
    fontSize: fontSizes.md,
    fontWeight: 600,
    color: '#374151'
})

export const input = style({
    height: '40px',
    padding: '0 12px',
    border: `1px solid ${colors.borderStrong}`,
    borderRadius: radii.sm,
    fontSize: fontSizes.md,
    outline: 'none',

    selectors: {
        '&:focus': {
            borderColor: colors.primary,
            boxShadow: '0 0 0 3px rgba(37, 99, 235, 0.15)'
        }
    }
})
