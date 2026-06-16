# API Folder

이 폴더는 React 코드가 Spring Boot 백엔드와 통신하는 함수들을 모아둔 곳입니다.

```text
component
-> hook
-> api function
-> backend endpoint
```

| 파일 | 역할 |
| --- | --- |
| `authApi.js` | 회원가입, 로그인 |
| `postApi.js` | 게시글, 댓글 CRUD |
| `ragApi.js` | 유사 게시글 검색, RAG 초안 생성 |
| `mcpApi.js` | MCP fact check |
| `agentApi.js` | 놓친 글 추천 Agent |

## 읽을 때 볼 점

- 어떤 endpoint로 요청을 보내는가
- JWT token을 header에 넣는가
- 실패 응답을 어디서 error로 바꾸는가
- 서버 응답을 화면에서 쓰기 좋게 normalize하는가

API 주소나 응답 형식이 바뀌면 보통 이 폴더를 먼저 봅니다.
