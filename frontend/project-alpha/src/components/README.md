# Components Folder

이 폴더는 재사용 가능한 UI 조각을 모아둡니다.

| 폴더/파일 | 역할 |
| --- | --- |
| `layout/Topbar.jsx` | 상단 사용자/로그아웃/검색 영역 |
| `layout/BoardSidebar.jsx` | 카테고리와 게시판 보조 정보 |
| `layout/PostFilterBar.jsx` | 카테고리/태그 필터와 글쓰기 진입 |
| `layout/Pagination.jsx` | 페이지 이동 버튼 |
| `posts/PostList.jsx` | 게시글 목록 렌더링 |
| `posts/PostCard.jsx` | 게시글 카드 한 개 |
| `posts/ComposerModal.jsx` | 글 작성 modal |
| `posts/PostForm.jsx` | 글 작성/수정 폼과 RAG 버튼 |
| `ai/FactCheckPanel.jsx` | MCP fact check 결과 표시 |
| `ai/AgentRecommendationsPanel.jsx` | Agent 추천 결과 표시 |
| `auth/AuthPanel.jsx` | 로그인 사용자와 로그아웃 버튼 |

## 읽을 때 볼 점

- 이 컴포넌트가 직접 상태를 갖는가, props로만 렌더링하는가
- 버튼 클릭이 어떤 handler prop으로 전달되는가
- AI 기능 결과가 일반 게시판 UI와 어떻게 분리되어 표시되는가

가능하면 component는 "어떻게 보일지"에 집중하고, "어떤 데이터를 가져올지"는 hook이나 API 계층에 맡깁니다.
