# Components Folder

이 폴더는 재사용 가능한 UI 조각을 모아둡니다.

| 파일 | 역할 |
| --- | --- |
| `Topbar.jsx` | 상단 사용자/로그아웃 영역 |
| `BoardSidebar.jsx` | 카테고리와 게시판 보조 정보 |
| `PostFilterBar.jsx` | 검색, 태그 필터 |
| `PostList.jsx` | 게시글 목록 렌더링 |
| `PostCard.jsx` | 게시글 카드 한 개 |
| `ComposerModal.jsx` | 글 작성 modal |
| `PostForm.jsx` | 글 작성/수정 폼과 RAG 버튼 |
| `FactCheckPanel.jsx` | MCP fact check 결과 표시 |
| `AgentRecommendationsPanel.jsx` | Agent 추천 결과 표시 |
| `Pagination.jsx` | 페이지 이동 버튼 |

## 읽을 때 볼 점

- 이 컴포넌트가 직접 상태를 갖는가, props로만 렌더링하는가
- 버튼 클릭이 어떤 handler prop으로 전달되는가
- AI 기능 결과가 일반 게시판 UI와 어떻게 분리되어 표시되는가

가능하면 component는 "어떻게 보일지"에 집중하고, "어떤 데이터를 가져올지"는 hook이나 API 계층에 맡깁니다.
