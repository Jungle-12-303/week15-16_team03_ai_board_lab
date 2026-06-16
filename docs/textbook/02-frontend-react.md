# 2장. React 프론트엔드

프론트엔드는 사용자가 실제로 만나는 화면입니다. Project Alpha의 React 코드는 화면 컴포넌트, 상태 관리 hook, API client를 분리합니다.

## 폴더 역할

| 폴더 | 역할 |
| --- | --- |
| `pages` | URL 단위 화면 |
| `components` | 재사용 가능한 화면 조각 |
| `hooks` | 상태 관리와 기능 흐름 |
| `api` | 백엔드 HTTP 요청 |
| `api` | Spring Boot 서버와 통신하는 fetch 함수 |
| `constants` | 카테고리 같은 고정 값 |

## 코드로 바로 이동

| 확인할 흐름 | 코드 위치 |
| --- | --- |
| 앱 진입과 페이지 선택 | [main.jsx](../../frontend/project-alpha/src/main.jsx), [App.jsx](../../frontend/project-alpha/src/App.jsx) |
| 로그인/회원가입 화면 | [LoginPage.jsx](../../frontend/project-alpha/src/pages/LoginPage.jsx), [SignupPage.jsx](../../frontend/project-alpha/src/pages/SignupPage.jsx) |
| 게시판 메인/상세 화면 | [BoardPage.jsx](../../frontend/project-alpha/src/pages/BoardPage.jsx), [PostDetailPage.jsx](../../frontend/project-alpha/src/pages/PostDetailPage.jsx) |
| 글 목록과 카드 | [PostList.jsx](../../frontend/project-alpha/src/components/PostList.jsx), [PostCard.jsx](../../frontend/project-alpha/src/components/PostCard.jsx) |
| 글 작성 모달 | [ComposerModal.jsx](../../frontend/project-alpha/src/components/ComposerModal.jsx), [PostForm.jsx](../../frontend/project-alpha/src/components/PostForm.jsx) |
| 인증 상태 | [useAuth.js](../../frontend/project-alpha/src/hooks/useAuth.js), [authApi.js](../../frontend/project-alpha/src/api/authApi.js) |
| 게시글 상태 | [usePosts.js](../../frontend/project-alpha/src/hooks/usePosts.js), [usePostComposer.js](../../frontend/project-alpha/src/hooks/usePostComposer.js) |
| RAG 상태 | [useRagDraft.js](../../frontend/project-alpha/src/hooks/useRagDraft.js), [ragApi.js](../../frontend/project-alpha/src/api/ragApi.js) |
| MCP/Agent 화면 | [FactCheckPanel.jsx](../../frontend/project-alpha/src/components/FactCheckPanel.jsx), [AgentRecommendationsPanel.jsx](../../frontend/project-alpha/src/components/AgentRecommendationsPanel.jsx) |

## 왜 custom hook을 썼나

React 컴포넌트 안에 API 호출, loading 상태, error 상태, form 상태를 모두 넣으면 파일이 빠르게 커집니다. 그래서 기능 단위로 custom hook을 만들었습니다.

| Hook | 책임 |
| --- | --- |
| `useAuth` | 로그인, 회원가입, 로그아웃, 현재 사용자 복원 |
| `usePosts` | 게시글 목록, CRUD, 댓글 상태 |
| `usePostComposer` | 글 작성/수정 모달의 입력 상태 |
| `useRagDraft` | 유사글 검색, RAG 초안 생성 상태 |
| `useAgentRecommendations` | 놓친 글 추천 요청 상태 |

이 분리는 컴포넌트와 데이터 흐름의 책임을 분리합니다. 화면 컴포넌트는 "무엇을 보여줄지"에 집중하고, hook은 "어떤 상태가 어떻게 변하는지"를 담당합니다. 서버 상태 캐싱, 전역 상태, 비동기 요청 정책이 복잡해지면 React Query, Zustand, Redux Toolkit 같은 도구를 검토할 수 있습니다.

## API client의 역할

`src/api` 폴더는 fetch 요청을 모아둔 곳입니다.

```text
component
-> hook
-> api function
-> Spring Boot endpoint
```

예를 들어 RAG 초안 생성은 다음 흐름입니다.

```text
PostForm.jsx
-> useRagDraft.createDraftFromSources
-> ragApi.createDraftFromSources
-> POST /api/ai/draft
```

이렇게 해두면 API 주소나 header 형식이 바뀌어도 화면 컴포넌트를 많이 건드리지 않아도 됩니다.

## 인증 상태

로그인 성공 후 JWT는 JavaScript에서 읽을 수 없는 httpOnly cookie로 저장됩니다. 이후 API 요청은 token 값을 직접 들고 다니지 않고 `credentials: 'include'` 옵션으로 쿠키를 함께 보냅니다.

이 방식은 SPA에서 토큰 전달 흐름이 명확합니다. 보안을 강화하려면 XSS 방어, refresh token, httpOnly cookie, token 만료 처리를 함께 설계해야 합니다.

## UI 설계 방향

처음에는 기능을 빠르게 확인하는 화면이었지만, 나중에는 게시판/글쓰기 흐름을 중심으로 정리했습니다.

- 메인은 글 목록 중심
- 글 작성은 modal로 분리
- RAG는 글쓰기 흐름 안에 배치
- MCP fact check는 이미 작성된 글의 상세 페이지에 배치
- Agent 추천은 메인에서 "놓친 글"을 확인하는 기능으로 배치

이 배치는 AI 기능을 별도 데모 화면에 모으지 않고, 글 작성과 글 검증 흐름 안에서 사용하게 합니다.

## 프론트엔드 적용 범위

- 현재는 서버 상태 캐싱 도구를 쓰지 않습니다.
- API 실패 재시도나 optimistic update는 구현 범위에 넣지 않았습니다.
- 접근성, 키보드 조작, 반응형 QA는 더 보강할 수 있습니다.
- form validation은 백엔드 validation에 많이 의존합니다.
