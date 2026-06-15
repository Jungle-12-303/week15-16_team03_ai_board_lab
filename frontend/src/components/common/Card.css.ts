import { style } from '@vanilla-extract/css'
import { colors, radii, space } from '../../styles/theme.css'

export const card = style({
    padding: space.xl,
    border: `1px solid ${colors.border}`,
    borderRadius: radii.md,
    backgroundColor: colors.surface
})
