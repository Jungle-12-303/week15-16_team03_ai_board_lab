# Hooks Folder

이 폴더는 React 화면에서 사용하는 상태 관리와 기능 흐름을 custom hook으로 분리한 곳입니다.

컴포넌트가 직접 API 호출, loading, error, form state를 모두 들고 있으면 금방 복잡해집니다. 그래서 Project Alpha는 기능별 hook을 둡니다.

| 파일 | 책임 |
| --- | --- |
| `useAuth.js` | 로그인, 회원가입, 로그아웃, 현재 사용자 복원 |
| `usePosts.js` | 게시글 목록, CRUD, 댓글 상태 |
| `usePostComposer.js` | 글 작성/수정 모달 입력 상태 |
| `useRagDraft.js` | 유사글 검색, 초안 생성 상태 |
| `useAgentRecommendations.js` | 놓친 글 추천 상태 |

## 읽을 때 볼 점

- `useState`로 어떤 값을 관리하는가
- API 요청 전후로 loading과 error가 어떻게 바뀌는가
- hook이 컴포넌트에 어떤 함수와 값을 반환하는가

면접식으로 설명하면, custom hook은 "화면에서 반복되는 상태 관리와 비동기 흐름을 기능 단위로 분리한 함수"입니다.
