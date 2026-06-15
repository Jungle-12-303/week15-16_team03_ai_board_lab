# 07-08 App2 Step 09: 작성/수정 정적 화면 만들기

## 공식문서 URL

- React - Writing Markup with JSX  
  https://react.dev/learn/writing-markup-with-jsx
- React DOM - `<form>`  
  https://react.dev/reference/react-dom/components/form
- React DOM - `<input>`  
  https://react.dev/reference/react-dom/components/input
- React DOM - `<textarea>`  
  https://react.dev/reference/react-dom/components/textarea
- MDN - HTML `<textarea>` element  
  https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/textarea

## 목적

게시글 작성과 수정에 공통으로 쓸 입력 화면을 만든다.

아직 state와 API를 붙이지 않고, 제목/본문/태그 입력 구조만 만든다.

## 성공 기준

- [x] 글쓰기 버튼을 누르면 작성 화면으로 이동할 자리가 있다.
- [x] 제목 입력칸이 있다.
- [x] 본문 textarea가 있다.
- [x] 태그 입력칸이 있다.
- [x] 저장 버튼과 취소 버튼이 있다.
- [ ] 아직 작성/수정 저장 동작은 없어도 된다.

## 작업 체크리스트

### 1. editor 화면 조건 만들기

- [x] `mode === 'edit'`일 때 editor 화면이 보이게 준비한다.
- [x] 또는 작성/수정 구분 전에는 임시로 editor 화면을 항상 렌더링해도 된다.
- [x] 목록, 상세, editor 화면이 동시에 섞이지 않도록 확인한다.

### 2. 작성 화면 UI 만들기

- [x] 제목 input을 만든다.
- [x] 본문 textarea를 만든다.
- [x] 태그 input을 만든다.
- [x] 태그 입력 예시는 쉼표 구분으로 안내한다.
- [x] 저장 버튼을 만든다.
- [x] 취소 버튼을 만든다.

### 3. 글쓰기 버튼 위치 연결하기

- [x] 목록 화면에 글쓰기 버튼을 둔다.
- [x] 버튼 클릭 시 작성 화면으로 이동할 계획을 세운다.
- [x] 아직 동작이 없어도 버튼 위치가 자연스러운지 확인한다.

### 4. 수정 화면 재사용 가능성 확인하기

- [x] 작성 화면과 수정 화면의 입력 필드가 같은지 확인한다.
- [x] 차이는 "초기값"과 "저장 API"뿐인지 확인한다.
- [x] 그래서 별도 화면 2개보다 하나의 editor 화면을 재사용하는 것이 맞는지 판단한다.

## 검증 체크리스트

- [x] 제목, 본문, 태그 입력 위치가 자연스러운지 확인한다.
- [x] 저장/취소 버튼이 form 아래에 있는지 확인한다.
- [x] 작성 화면과 수정 화면을 같은 UI로 써도 어색하지 않은지 확인한다.
- [x] 로그인하지 않은 사용자에게 글쓰기 버튼을 어떻게 보여줄지 기준을 정한다.

## WHY 정리 질문

- [ ] 왜 작성과 수정 화면을 따로 만들지 않고 같은 editor UI로 볼 수 있는가?
- [ ] 왜 태그 입력은 배열이 아니라 문자열 input으로 먼저 받는가?
- [ ] 왜 이 단계에서는 아직 `editor` state를 만들지 않아도 되는가?
- [ ] 왜 저장 API보다 입력 구조 확인이 먼저인가?

## 다음 단계

- [ ] `07-08-app2-10-editor-state-save-branch.md`로 넘어간다.
