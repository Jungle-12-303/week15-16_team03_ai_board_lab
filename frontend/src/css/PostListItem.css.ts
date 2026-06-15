import { style } from '@vanilla-extract/css'

export const item = style({
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
    padding: '20px',
    border: '1px solid #e5e7eb',
    borderRadius: '8px',
    backgroundColor: '#ffffff',
    cursor: 'pointer',
    transition: 'border-color 150ms ease, box-shadow 150ms ease, transform 150ms ease',
    selectors: {
        '&:hover': {
            borderColor: '#2563eb',
            boxShadow: '0 10px 28px rgba(25, 36, 58, 0.08)',
            transform: 'translateY(-1px)'
        },
        '&:focus-visible': {
            outline: '2px solid #2563eb',
            outlineOffset: '2px'
        }
    }
})

export const title = style({
    margin: 0,
    fontSize: '20px',
    fontWeight: 800,
    color: '#111827'
})

export const preview = style({
    margin: 0,
    color: '#4b5563',
    fontSize: '14px',
    lineHeight: 1.6
})

export const meta = style({
    display: 'flex',
    flexWrap: 'wrap',
    gap: '8px',
    color: '#6b7280',
    fontSize: '13px'
})

export const tags = style({
    display: 'flex',
    flexWrap: 'wrap',
    gap: '6px'
})

export const tag = style({
    display: 'inline-flex',
    alignItems: 'center',
    height: '26px',
    padding: '0 8px',
    borderRadius: '999px',
    backgroundColor: '#eff6ff',
    color: '#1d4ed8',
    fontSize: '12px',
    fontWeight: 700
})
