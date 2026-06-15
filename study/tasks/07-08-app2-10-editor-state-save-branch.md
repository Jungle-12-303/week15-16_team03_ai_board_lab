# 07-08 App2 Step 10: 작성/수정 editor state와 저장 분기 흐름 만들기

## 공식문서 URL

- React - State: A Component's Memory  
  https://react.dev/learn/state-a-components-memory
- React - Reacting to Input with State  
  https://react.dev/learn/reacting-to-input-with-state
- React - Updating Objects in State  
  https://react.dev/learn/updating-objects-in-state
- React - Choosing the State Structure  
  https://react.dev/learn/choosing-the-state-structure
- React DOM - `<textarea>`  
  https://react.dev/reference/react-dom/components/textarea
- MDN - String.prototype.split()  
  https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/split

## 목적

작성/수정 입력값을 React state로 관리하고, 저장 버튼을 눌렀을 때 작성인지 수정인지 구분하는 흐름을 만든다.

아직 API를 호출하지 않고 `console.log`로 payload와 분기 결과를 확인한다.

## 성공 기준

- [x] `editor` state가 제목, 본문, 태그 문자열을 관리한다.
- [x] `editingId` state로 작성과 수정을 구분한다.
- [x] 글쓰기 버튼 클릭 시 빈 editor로 초기화된다.
- [x] 수정 버튼 클릭 시 기존 게시글 값이 editor에 채워진다.
- [x] 저장 submit 시 작성 payload와 수정 payload가 구분된다.

## 작업 체크리스트

### 1. editor state 만들기

- [x] `editor` state를 만든다.
- [x] `editor.title`을 제목 input에 연결한다.
- [x] `editor.content`를 본문 textarea에 연결한다.
- [x] `editor.tagsText`를 태그 input에 연결한다.
- [x] input 변경 시 해당 필드만 갱신한다.

### 2. editingId state 만들기

- [x] `editingId` state를 만든다.
- [x] 초기값은 `null`로 둔다.
- [x] `editingId === null`이면 작성 모드로 본다.
- [x] `editingId !== null`이면 수정 모드로 본다.

### 3. startCreate 함수 만들기

- [x] `startCreate()` 함수를 만든다.
- [x] 로그인하지 않았으면 메시지를 보여주고 중단한다.
- [x] `editingId`를 `null`로 초기화한다.
- [x] `editor`를 빈 값으로 초기화한다.
- [x] `mode`를 `'edit'`로 바꾼다.

### 4. startEdit 함수 만들기

- [x] `startEdit(post)` 함수를 만든다.
- [x] `editingId`를 `post.id`로 설정한다.
- [x] `editor.title`에 기존 제목을 넣는다.
- [x] `editor.content`에 기존 본문을 넣는다.
- [x] `editor.tagsText`에 기존 태그들을 쉼표 문자열로 넣는다.
- [x] `mode`를 `'edit'`로 바꾼다.

### 5. 저장 submit 흐름 만들기

- [x] `handleEditorSubmit(event)`를 만든다.
- [x] `event.preventDefault()`를 호출한다.
- [x] 제목과 본문이 비어 있으면 저장을 막는다.
- [x] `tagsText`를 쉼표 기준으로 나눠 배열로 만든다.
- [x] 공백 태그를 제거한다.
- [x] 작성 모드이면 create payload를 콘솔에 찍는다.
- [x] 수정 모드이면 update id와 payload를 콘솔에 찍는다.

## 검증 체크리스트

- [ ] 글쓰기 버튼 클릭 시 빈 editor 화면이 보이는지 확인한다.
- [ ] 수정 버튼 클릭 시 기존 게시글 값이 editor에 들어오는지 확인한다.
- [ ] 저장 시 작성 모드와 수정 모드가 다르게 콘솔에 찍히는지 확인한다.
- [ ] 태그 문자열이 배열로 바뀌는지 확인한다.
- [ ] 제목이나 본문이 비었을 때 저장이 막히는지 확인한다.

## WHY 정리 질문

- [ ] 왜 작성과 수정을 `editingId` 하나로 구분할 수 있는가?
- [ ] 왜 `editor`를 제목, 본문, 태그 문자열 객체로 묶는가?
- [ ] 왜 태그는 저장 직전에 배열로 변환하는가?
- [ ] 왜 API 연결 전에 console로 분기 흐름을 확인하는가?

## 다음 단계

- [ ] `07-08-app2-11-editor-api-refresh.md`로 넘어간다.
