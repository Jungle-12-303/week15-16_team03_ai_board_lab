# 07-08 App2 재구현 로드맵: 기능별로 화면 -> 상태 -> API -> 리팩토링

## 공식문서 URL

- React - Thinking in React  
  https://react.dev/learn/thinking-in-react
- React - Your UI as a Tree  
  https://react.dev/learn/understanding-your-ui-as-a-tree
- React - State: A Component's Memory  
  https://react.dev/learn/state-a-components-memory
- React - Choosing the State Structure  
  https://react.dev/learn/choosing-the-state-structure
- React - Synchronizing with Effects  
  https://react.dev/learn/synchronizing-with-effects
- Vite - Getting Started  
  https://vite.dev/guide/
- Vite - Env Variables and Modes  
  https://vite.dev/guide/env-and-mode
- Axios - Creating an instance  
  https://axios-http.com/docs/instance
- Axios - Interceptors  
  https://axios-http.com/docs/interceptors

## 목적

`frontend/src/App2.jsx`를 처음부터 다시 만들 때 한 번에 전체 앱을 완성하지 않는다.

기능 하나를 고르고 아래 순서로 끝까지 연결한다.

```txt
화면 만들기
-> 필요한 state 붙이기
-> console.log 또는 임시 데이터로 흐름 확인
-> axios 직접 호출로 API 연결
-> 반복이 보이면 client.js로 분리
```

## 성공 기준

- [ ] 로그인/회원가입, 목록, 상세, 작성/수정 기능을 각각 독립된 학습 단위로 설명할 수 있다.
- [ ] 각 기능마다 "왜 이 state가 필요한지"를 말할 수 있다.
- [ ] 처음부터 `client.js`로 분리하지 않고, 반복을 확인한 뒤 분리할 수 있다.
- [ ] `App2.jsx`가 왜 MVP 학습용 중심 파일이 되는지 설명할 수 있다.

## 전체 진행 순서

- [x] 01. 로그인/회원가입 정적 화면을 만든다.
- [x] 02. 로그인/회원가입 form state와 submit 흐름을 만든다.
- [x] 03. 로그인/회원가입 API와 localStorage 흐름을 붙인다.
- [x] 04. 게시글 목록 정적 화면을 만든다.
- [x] 05. 게시글 목록 state, 검색 state, 임시 조회 흐름을 만든다.
- [x] 06. 게시글 목록 API를 붙인다.
- [x] 07. 게시글 상세 화면과 `mode`, `selectedPost` 흐름을 만든다.
- [x] 08. 게시글 상세 API와 작성자 본인 버튼 조건을 붙인다.
- [x] 09. 작성/수정 정적 화면을 만든다.
- [x] 10. 작성/수정 editor state와 저장 분기 흐름을 만든다.
- [x] 11. 작성/수정 API와 저장 후 재조회 흐름을 붙인다.
- [x] 12. AI 참고 결과 패널을 붙인다.
- [x] 13. 반복되는 axios 코드를 `client.js`로 분리한다.

## 작업 원칙

- [ ] 한 task에서 한 기능 단계만 끝낸다.
- [ ] API가 없어도 화면 흐름을 먼저 확인한다.
- [ ] 아직 필요하지 않은 라우터, Redux, Zustand, React Query를 도입하지 않는다.
- [ ] 컴포넌트 파일 분리는 지금 하지 않는다.
- [ ] `App2.jsx` 안에서 흐름을 먼저 읽히게 만든다.
- [ ] 반복이 실제로 보이기 전에는 추상화하지 않는다.

## WHY 정리 질문

- [ ] 왜 전체 앱을 한 번에 만들지 않고 기능별로 쪼개는가?
- [ ] 왜 화면을 먼저 만들고 API를 나중에 붙이는가?
- [ ] 왜 MVP에서는 `App2.jsx` 하나에 흐름을 모아도 되는가?
- [ ] 왜 리팩토링은 처음이 아니라 마지막에 하는가?
