# Pages Folder

이 폴더는 URL 단위 화면을 조립합니다.

| 파일 | 역할 |
| --- | --- |
| `LoginPage.jsx` | 로그인 화면 |
| `SignupPage.jsx` | 회원가입 화면 |
| `BoardPage.jsx` | 메인 게시판 화면 |
| `PostDetailPage.jsx` | 게시글 상세, 댓글, MCP fact check 화면 |

Page는 되도록 화면 조립에 집중합니다. 데이터 요청과 복잡한 상태 관리는 `hooks`로 빼고, 버튼이나 카드 같은 작은 화면 조각은 `components`로 나눕니다.

## 읽을 때 볼 점

- 이 화면이 어떤 hook 값을 받는가
- 어떤 component를 조립하는가
- 사용자 이벤트가 어떤 handler로 이어지는가
- URL parameter나 navigation을 어디서 사용하는가
