import { style } from '@vanilla-extract/css'
import { colors, radii, space } from '../styles/theme.css'

export const notice = style({
    margin: `0 0 ${space.lg}`,
    border: `1px solid ${colors.noticeBorder}`,
    borderRadius: radii.md,
    padding: `${space.md} ${space.lg}`,
    color: colors.noticeText,
    backgroundColor: colors.noticeBackground,
    fontSize: '14px',
    fontWeight: 700
})

export const dashboard = style({
    display: 'grid',
    gridTemplateColumns: '350px minmax(0, 1fr)',
    alignItems: 'start',
    gap: '20px',
    width: '100%',
    '@media': {
        'screen and (max-width: 900px)': {
            gridTemplateColumns: '1fr'
        }
    }
})

export const authColumn = style({
    position: 'sticky',
    top: '20px',
    display: 'grid',
    gap: space.lg,
    minWidth: 0
})

export const contentColumn = style({
    minWidth: 0
})
