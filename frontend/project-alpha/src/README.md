# Frontend Source Guide

React 프론트엔드의 출발점입니다.

처음 읽을 때는 아래 순서가 좋습니다.

```text
main.jsx
-> App.jsx
-> pages
-> hooks
-> api
-> components
```

## 폴더 역할

| 폴더 | 책임 |
| --- | --- |
| `pages` | URL 단위 화면 조립 |
| `components` | 재사용 가능한 UI 조각 |
| `hooks` | 상태 관리와 기능 흐름 |
| `api` | Spring Boot API 호출 |
| `storage` | 브라우저 저장소 접근 |
| `constants` | 카테고리 같은 고정값 |

Project Alpha의 프론트엔드는 AI 기능을 게시글 작성과 상세 검증 흐름 안에서 사용할 수 있게 배치합니다.
