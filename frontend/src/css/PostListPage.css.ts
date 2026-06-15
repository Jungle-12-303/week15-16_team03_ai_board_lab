import { style } from '@vanilla-extract/css'

export const page = style({
    display: 'flex',
    flexDirection: 'column',
    gap: '18px'
})

export const header = style({
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: '12px'
})

export const title = style({
    margin: 0,
    fontSize: '28px',
    fontWeight: 800,
    color: '#111827'
})

export const toolbar = style({
    display: 'flex',
    alignItems: 'center',
    gap: '10px'
})

export const searchForm = style({
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    width: '100%',
    maxWidth: '420px'
})

export const searchInput = style({
    flex: 1,
    height: '40px',
    padding: '0 12px',
    border: '1px solid #d1d5db',
    borderRadius: '6px',
    fontSize: '14px',
    outline: 'none',
    selectors: {
        '&:focus': {
            borderColor: '#2563eb',
            boxShadow: '0 0 0 3px rgba(37, 99, 235, 0.15)'
        }
    }
})

export const button = style({
    height: '40px',
    padding: '0 14px',
    border: 'none',
    borderRadius: '6px',
    backgroundColor: '#2563eb',
    color: '#ffffff',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: 700,
    selectors: {
        '&:hover': {
            backgroundColor: '#1d4ed8'
        }
    }
})

export const list = style({
    display: 'flex',
    flexDirection: 'column',
    gap: '12px'
})

export const loading = style({
    margin: 0,
    color: '#657188',
    fontSize: '14px',
    fontWeight: 700
})

export const empty = style({
    padding: '24px',
    border: '1px dashed #d1d5db',
    borderRadius: '8px',
    backgroundColor: '#ffffff',
    color: '#6b7280',
    textAlign: 'center'
})

export const pagination = style({
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '8px',
    paddingTop: '4px'
})

export const pageButton = style({
    minWidth: '36px',
    height: '36px',
    border: '1px solid #c8d3e1',
    borderRadius: '8px',
    backgroundColor: '#ffffff',
    color: '#657188',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: 700,
    selectors: {
        '&:hover': {
            borderColor: '#2563eb',
            color: '#2563eb',
            backgroundColor: '#eaf1ff'
        },
        '&:disabled': {
            cursor: 'not-allowed',
            opacity: 0.45
        }
    }
})

export const activePageButton = style({
    borderColor: '#2563eb',
    color: '#ffffff',
    backgroundColor: '#2563eb',
    selectors: {
        '&:hover': {
            color: '#ffffff',
            backgroundColor: '#1d4ed8'
        }
    }
})

export const detail = style({
    display: 'flex',
    flexDirection: 'column',
    gap: '16px'
})

export const detailHeader = style({
    display: 'flex',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: '12px'
})

export const detailTitle = style({
    margin: 0,
    color: '#111827',
    fontSize: '24px',
    fontWeight: 800,
    lineHeight: 1.3
})

export const ownerActions = style({
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    flexShrink: 0
})

export const meta = style({
    display: 'flex',
    flexWrap: 'wrap',
    gap: '8px',
    color: '#6b7280',
    fontSize: '13px'
})

export const detailContent = style({
    margin: 0,
    color: '#172033',
    fontSize: '15px',
    lineHeight: 1.7,
    whiteSpace: 'pre-wrap'
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

export const editor = style({
    display: 'flex',
    flexDirection: 'column',
    gap: '16px'
})

export const field = style({
    display: 'flex',
    flexDirection: 'column',
    gap: '6px'
})

export const labelText = style({
    color: '#374151',
    fontSize: '14px',
    fontWeight: 700
})

export const textarea = style({
    width: '100%',
    minHeight: '220px',
    padding: '12px',
    border: '1px solid #d1d5db',
    borderRadius: '6px',
    color: '#172033',
    fontSize: '14px',
    lineHeight: 1.7,
    resize: 'vertical',
    outline: 'none',
    selectors: {
        '&:focus': {
            borderColor: '#2563eb',
            boxShadow: '0 0 0 3px rgba(37, 99, 235, 0.15)'
        }
    }
})

export const formActions = style({
    display: 'flex',
    flexWrap: 'wrap',
    gap: '8px'
})

export const aiPanel = style({
    display: 'flex',
    flexDirection: 'column',
    gap: '14px'
})

export const aiHeader = style({
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: '12px'
})

export const mcpTool = style({
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    maxWidth: '520px'
})

export const panelTitle = style({
    margin: 0,
    color: '#111827',
    fontSize: '18px',
    fontWeight: 800
})

export const aiResult = style({
    display: 'flex',
    flexDirection: 'column',
    gap: '8px'
})

export const resultTitle = style({
    margin: 0,
    color: '#111827',
    fontSize: '15px',
    fontWeight: 800
})

export const resultHeader = style({
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: '8px'
})

export const resultPre = style({
    maxWidth: '100%',
    overflowX: 'auto',
    margin: 0,
    padding: '12px',
    border: '1px solid #dfe6ef',
    borderRadius: '8px',
    color: '#243044',
    backgroundColor: '#f8fafc',
    fontSize: '13px',
    lineHeight: 1.6,
    whiteSpace: 'pre-wrap',
    wordBreak: 'break-word'
})

export const comments = style({
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
    paddingTop: '8px',
    borderTop: '1px solid #dfe6ef'
})

export const comment = style({
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-start',
    gap: '6px',
    padding: '10px 12px',
    borderRadius: '8px',
    color: '#374151',
    backgroundColor: '#f8fafc',
    fontSize: '14px',
    lineHeight: 1.5
})

export const commentMeta = style({
    display: 'flex',
    flexWrap: 'wrap',
    gap: '8px',
    color: '#6b7280',
    fontSize: '12px',
    fontWeight: 700
})

export const commentContent = style({
    margin: 0,
    color: '#172033',
    fontSize: '14px',
    lineHeight: 1.6,
    whiteSpace: 'pre-wrap'
})

export const commentForm = style({
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
    paddingTop: '8px',
    borderTop: '1px solid #dfe6ef'
})

export const commentTextarea = style({
    width: '100%',
    minHeight: '90px',
    padding: '10px 12px',
    border: '1px solid #d1d5db',
    borderRadius: '6px',
    color: '#172033',
    fontSize: '14px',
    lineHeight: 1.6,
    resize: 'vertical',
    outline: 'none',
    selectors: {
        '&:focus': {
            borderColor: '#2563eb',
            boxShadow: '0 0 0 3px rgba(37, 99, 235, 0.15)'
        }
    }
})

export const chatPanel = style({
    display: 'flex',
    flexDirection: 'column',
    gap: '14px'
})

export const chatHeader = style({
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: '12px'
})

export const chatMessages = style({
    display: 'flex',
    flexDirection: 'column',
    gap: '10px',
    maxHeight: '360px',
    overflowY: 'auto',
    padding: '12px',
    border: '1px solid #dfe6ef',
    borderRadius: '8px',
    backgroundColor: '#f8fafc'
})

export const chatEmpty = style({
    margin: 0,
    color: '#657188',
    fontSize: '14px',
    fontWeight: 700,
    textAlign: 'center'
})

export const chatMessage = style({
    display: 'flex'
})

export const chatUserMessage = style({
    justifyContent: 'flex-end'
})

export const chatAssistantMessage = style({
    justifyContent: 'flex-start'
})

export const chatBubble = style({
    display: 'flex',
    flexDirection: 'column',
    gap: '10px',
    maxWidth: '78%',
    padding: '10px 12px',
    borderRadius: '8px',
    fontSize: '14px',
    lineHeight: 1.6,
    wordBreak: 'break-word',
    '@media': {
        'screen and (max-width: 640px)': {
            maxWidth: '100%'
        }
    }
})

export const chatUserBubble = style({
    backgroundColor: '#2563eb',
    color: '#ffffff'
})

export const chatAssistantBubble = style({
    border: '1px solid #dbe4f0',
    backgroundColor: '#ffffff',
    color: '#172033'
})

export const chatContent = style({
    margin: 0,
    whiteSpace: 'pre-wrap'
})

export const chatSources = style({
    display: 'flex',
    flexDirection: 'column',
    gap: '6px',
    paddingTop: '8px',
    borderTop: '1px solid #e5e7eb'
})

export const chatSourcesTitle = style({
    margin: 0,
    color: '#374151',
    fontSize: '12px',
    fontWeight: 800
})

export const sourceButton = style({
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-start',
    gap: '4px',
    width: '100%',
    padding: '8px 10px',
    border: '1px solid #bfdbfe',
    borderRadius: '6px',
    backgroundColor: '#eff6ff',
    color: '#1f2937',
    cursor: 'pointer',
    textAlign: 'left',
    selectors: {
        '&:hover': {
            borderColor: '#2563eb',
            backgroundColor: '#dbeafe'
        }
    }
})

export const sourceButtonTitle = style({
    color: '#1d4ed8',
    fontSize: '13px',
    fontWeight: 800
})

export const sourceButtonPreview = style({
    color: '#4b5563',
    fontSize: '12px',
    lineHeight: 1.5
})

export const chatForm = style({
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    '@media': {
        'screen and (max-width: 640px)': {
            alignItems: 'stretch',
            flexDirection: 'column'
        }
    }
})

export const chatInput = style({
    flex: 1,
    minWidth: 0,
    height: '40px',
    padding: '0 12px',
    border: '1px solid #d1d5db',
    borderRadius: '6px',
    color: '#172033',
    fontSize: '14px',
    outline: 'none',
    selectors: {
        '&:focus': {
            borderColor: '#2563eb',
            boxShadow: '0 0 0 3px rgba(37, 99, 235, 0.15)'
        }
    }
})
