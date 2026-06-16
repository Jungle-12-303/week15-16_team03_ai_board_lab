# 5장. MCP와 Agent

MCP와 Agent는 둘 다 AI 기능의 실행 흐름을 다루지만 역할이 다릅니다.

| 개념 | 이 프로젝트에서의 역할 |
| --- | --- |
| MCP | 외부 시스템을 도구처럼 호출하는 통로 |
| Agent | 상태를 보고 어떤 데이터를 추천할지 단계적으로 판단하는 흐름 |

## MCP를 글 검증 기능으로 둔 이유

RAG는 내부 게시글을 근거로 초안을 만드는 기능입니다. MCP까지 글 생성을 담당하면 두 기능의 경계가 흐려집니다. Project Alpha에서는 MCP를 "이미 작성된 글의 fact check"에 사용합니다.

```text
RAG: 내부 게시글을 근거로 초안 생성
MCP: 외부 API 데이터를 가져와 작성된 글 검증
```

이렇게 나누면 내부 지식 활용과 외부 데이터 검증이 분리됩니다.

## MCP 구현 구조

```text
PostDetailPage.jsx
-> mcpApi.checkFact
-> PostFactCheckController
-> McpFactCheckService
-> GitHubFactCheckService / WeatherFactCheckService
-> McpServerService
-> GitHubApiClient / WeatherApiClient
```

`McpServerService`는 JSON-RPC 방식의 `tools/list`, `tools/call`을 처리합니다. 별도 프로세스가 아니라 Spring Boot 애플리케이션 안에서 MCP endpoint를 제공합니다.

## 코드로 바로 이동

| 확인할 흐름 | 코드 위치 |
| --- | --- |
| 상세 화면 fact check UI | [PostDetailPage.jsx](../../frontend/project-alpha/src/pages/PostDetailPage.jsx), [FactCheckPanel.jsx](../../frontend/project-alpha/src/components/ai/FactCheckPanel.jsx), [mcpApi.js](../../frontend/project-alpha/src/api/ai/mcpApi.js) |
| MCP API 입구 | [PostFactCheckController.java](../../backend/src/main/java/com/jungle_choi/namanmu/api/PostFactCheckController.java), [McpController.java](../../backend/src/main/java/com/jungle_choi/namanmu/api/McpController.java) |
| MCP JSON-RPC 처리 | [McpServerService.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/mcp/McpServerService.java) |
| Fact check 조합 | [McpFactCheckService.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/mcp/McpFactCheckService.java) |
| GitHub 도구 | [GitHubFactCheckService.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/mcp/GitHubFactCheckService.java), [GitHubApiClient.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/mcp/GitHubApiClient.java) |
| Weather 도구 | [WeatherFactCheckService.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/mcp/WeatherFactCheckService.java), [WeatherApiClient.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/mcp/WeatherApiClient.java) |
| Agent 추천 UI | [AgentRecommendationsPanel.jsx](../../frontend/project-alpha/src/components/ai/AgentRecommendationsPanel.jsx), [useAgentRecommendations.js](../../frontend/project-alpha/src/hooks/useAgentRecommendations.js), [agentApi.js](../../frontend/project-alpha/src/api/ai/agentApi.js) |
| Agent 추천 로직 | [AgentController.java](../../backend/src/main/java/com/jungle_choi/namanmu/api/AgentController.java), [AgentRecommendationService.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/agent/AgentRecommendationService.java), [PostReadService.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/post/PostReadService.java) |

## MCP 도구

| 도구 | 외부 API | 쓰임 |
| --- | --- | --- |
| `github.repository_summary` | GitHub REST API | 저장소명, 설명, 언어, 라이선스, 기본 브랜치 확인 |
| `weather.current_forecast` | wttr.in 기반 weather API | 지역 날씨 주장 확인 |

GitHub token은 선택 사항입니다. token 없이도 공개 저장소는 조회할 수 있지만 rate limit에 걸릴 수 있습니다.

## Fact check 결과

팩트체크 응답은 다음 요소를 가집니다.

| 필드 | 의미 |
| --- | --- |
| `claim` | 글에서 확인하려는 주장 |
| `verdict` | supported, contradicted, uncertain, too vague 등 |
| `comparison` | 외부 데이터와 글 내용 비교 |
| `suggestion` | 더 안전한 표현 제안 |
| `externalFact` | MCP 도구로 가져온 원본 요약 |

중요한 점은 외부 데이터를 단순히 나열하지 않는 것입니다. 글의 실제 문장과 외부 데이터를 비교해야 MCP 기능의 가치가 생깁니다.

## Agent 기능

Agent는 놓친 글 5개를 추천합니다.

```text
사용자 읽음 기록 관찰
-> 관심 태그/카테고리 추론
-> 읽지 않은 후보 검색
-> 점수화와 정렬
-> 5개 추천
```

이 프로젝트의 Agent는 LangGraph 같은 별도 프레임워크를 쓰지 않습니다. 대신 agent loop의 핵심 요소를 직접 코드로 표현했습니다.

| 단계 | 설명 |
| --- | --- |
| observe | 최근 읽은 글 50개 확인 |
| infer | 자주 본 태그와 카테고리로 관심사 추론 |
| retrieve | 아직 읽지 않은 후보 글 조회 |
| rank | 태그, 카테고리, 최신성 기준으로 점수화 |

## 왜 최근 읽은 글을 제한하나

읽음 기록을 무한히 추천 근거로 쓰면 오래된 관심사가 계속 영향을 줄 수 있습니다. 그래서 최근 읽은 글 중심으로 관심사를 봅니다.

추천에 사용하는 신호는 다음과 같습니다.

- 최근 읽은 글 기반
- 읽은 글 제외
- 태그/카테고리/최신성 점수
- 추천 이유 단계 제공

확장 시에는 클릭, 댓글, 체류 시간, 숨김 처리, 유저 피드백을 추천 신호로 추가할 수 있습니다.

## MCP/Agent 적용 범위와 확장

MCP 적용 범위:

- 별도 MCP client/server 프로세스 배포 구조는 아닙니다.
- 도구가 GitHub와 날씨로 제한되어 있습니다.
- 외부 API 장애나 rate limit 대응은 기본 오류 처리 중심입니다.

Agent 적용 범위:

- 장기/단기 관심사를 분리하지 않습니다.
- LLM이 도구를 스스로 여러 번 호출하는 복잡한 loop는 아닙니다.
- 추천 품질 정량 평가는 제한적입니다.

확장 후보:

- Jira, Slack, 공공데이터 API MCP 도구 추가
- LangGraph 기반 multi-step agent
- user feedback 기반 추천 점수 보정
- 추천 결과의 precision/recall 평가
