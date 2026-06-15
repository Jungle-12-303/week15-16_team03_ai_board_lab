import { style } from '@vanilla-extract/css'
import { colors, fontSizes, space } from '../styles/theme.css'

export const authPanel = style({
    display: 'grid',
    gap: space.md,
    width: '100%'
})

export const switchBox = style({
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: space.sm,
    width: '100%',
    maxWidth: '360px'
})

export const switchText = style({
    margin: 0,
    color: colors.muted,
    fontSize: fontSizes.sm
})

export const userCard = style({
    display: 'grid',
    gap: space.sm,
    width: '100%',
    maxWidth: '280px'
})

export const kicker = style({
    margin: 0,
    color: colors.accent,
    fontSize: fontSizes.sm,
    fontWeight: 700
})

export const userName = style({
    margin: 0,
    color: colors.text,
    fontSize: fontSizes.xxl,
    fontWeight: 800
})

export const userEmail = style({
    margin: 0,
    color: colors.muted,
    fontSize: fontSizes.md
})
