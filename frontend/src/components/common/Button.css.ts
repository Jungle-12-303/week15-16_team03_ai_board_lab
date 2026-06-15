import { style, styleVariants } from '@vanilla-extract/css'
import { colors, fontSizes, radii } from '../../styles/theme.css'

export const button = style({
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '6px',
    border: 'none',
    borderRadius: radii.sm,
    cursor: 'pointer',
    fontWeight: 700,
    lineHeight: 1,
    whiteSpace: 'nowrap',
    selectors: {
        '&:focus-visible': {
            outline: `2px solid ${colors.primary}`,
            outlineOffset: '2px'
        },
        '&:disabled': {
            cursor: 'not-allowed',
            opacity: 0.6
        }
    }
})

export const variant = styleVariants({
    primary: {
        backgroundColor: colors.primary,
        color: colors.surface,
        selectors: {
            '&:hover:not(:disabled)': {
                backgroundColor: colors.primaryHover
            }
        }
    },
    secondary: {
        border: `1px solid ${colors.borderStrong}`,
        backgroundColor: colors.surfaceMuted,
        color: colors.text,
        selectors: {
            '&:hover:not(:disabled)': {
                backgroundColor: colors.primarySoft
            }
        }
    },
    subtle: {
        border: `1px solid #bfdbfe`,
        backgroundColor: colors.primarySoft,
        color: colors.primaryHover,
        selectors: {
            '&:hover:not(:disabled)': {
                backgroundColor: colors.primarySoftHover
            }
        }
    }
})

export const size = styleVariants({
    sm: {
        minHeight: '28px',
        padding: '0 10px',
        fontSize: fontSizes.xs
    },
    md: {
        minHeight: '40px',
        padding: '0 14px',
        fontSize: fontSizes.md
    },
    lg: {
        minHeight: '42px',
        padding: '0 16px',
        fontSize: fontSizes.lg
    }
})
