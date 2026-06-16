# MCP Server

이 폴더는 게시판 프로젝트의 MCP 기능을 분리해서 구현한 서버입니다.

현재 구현된 도구:

- `create_weather_briefing_draft`
  - 도시 이름을 입력하면
  - Open-Meteo 날씨 데이터를 조회하고
  - 게시판에 바로 올릴 수 있는 날씨 브리핑 초안을 만들어줍니다.

## 왜 따로 분리했는가

이번 과제에서 MCP는 "외부 도구를 호출하는 서버"라는 구조를 보여주는 것이 중요합니다.
그래서 기존 Spring Boot 백엔드와 섞지 않고, `mcp-server/`를 별도 실행 구조로 분리했습니다.

## 설치

```bash
cd mcp-server
npm install
```

설치할 패키지:

- `@modelcontextprotocol/sdk`
- `zod`
- `typescript`
- `tsx`
- `@types/node`

## 실행

개발 실행:

```bash
npm run dev
```

빌드 후 실행:

```bash
npm run build
npm run start
```

## 에이전트 실행

이 프로젝트에서는 MCP 서버만 만든 것이 아니라, 그 서버를 실제로 호출하는 간단한 에이전트 클라이언트도 같이 넣었습니다.

실행:

```bash
npm run agent:weather -- Seoul 2
```

의미:

- `Seoul`: 도시 이름
- `2`: 예보 일수

이 명령은 내부적으로 아래 순서로 동작합니다.

1. TypeScript 코드를 빌드합니다.
2. MCP 서버를 stdio 방식으로 실행합니다.
3. 에이전트 클라이언트가 서버에 연결합니다.
4. `create_weather_briefing_draft` 도구를 호출합니다.
5. 결과를 게시글 초안 형태로 출력합니다.

## Inspector로 테스트

```bash
npx @modelcontextprotocol/inspector node dist/index.js
```

Inspector가 열리면 `create_weather_briefing_draft` 도구를 호출할 수 있습니다.

예시 입력:

```json
{
  "city": "Seoul",
  "forecastDays": 2
}
```

## 도구 반환 형태

도구는 게시글 초안에 필요한 내용을 하나의 텍스트로 돌려줍니다.
안에는 아래 정보가 포함됩니다.

- 추천 제목
- 게시글 본문 초안
- 추천 태그
- 참고한 날씨 데이터 요약
