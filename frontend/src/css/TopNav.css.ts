import { style } from '@vanilla-extract/css'

export const topbar = style({
    minHeight: '72px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: '16px',
    padding: '16px 24px',
    borderBottom: '1px solid #e5e7eb',
    backgroundColor: '#ffffff'
})

export const brand = style({
    display: 'flex',
    flexDirection: 'column',
    gap: '4px'
})

export const eyebrow = style({
    margin: 0,
    fontSize: '12px',
    fontWeight: 700,
    color: '#2563eb',
    letterSpacing: '0'
})

export const title = style({
    margin: 0,
    fontSize: '22px',
    fontWeight: 800,
    color: '#111827'
})

export const actions = style({
    display: 'flex',
    alignItems: 'center',
    gap: '8px'
})

export const userChip = style({
    display: 'inline-flex',
    alignItems: 'center',
    gap: '6px',
    height: '34px',
    padding: '0 12px',
    border: '1px solid #d1d5db',
    borderRadius: '999px',
    backgroundColor: '#f9fafb',
    color: '#374151',
    fontSize: '13px',
    fontWeight: 600,
    whiteSpace: 'nowrap'
})

export const button = style({
    minHeight: '28px',
    padding: '0 10px',
    border: '1px solid #bfdbfe',
    borderRadius: '6px',
    backgroundColor: '#eff6ff',
    color: '#1d4ed8',
    cursor: 'pointer',
    fontSize: '12px',
    fontWeight: 700,
    lineHeight: 1,
    selectors: {
        '&:hover': {
            backgroundColor: '#dbeafe'
        },
        '&:focus-visible': {
            outline: '2px solid #2563eb',
            outlineOffset: '2px'
        }
    }
})
