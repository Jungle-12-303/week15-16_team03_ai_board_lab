# Project Alpha RAGAS Evaluation

Project Alpha의 RAG 결과를 RAGAS로 평가하기 위한 독립 실행 스크립트입니다.

## 무엇을 평가하나

이 스크립트는 Spring Boot 서버의 RAG 초안 생성 API를 호출합니다.

```text
POST /api/ai/draft
```

MCP 팩트체크나 Agent 추천 기능이 아니라, 게시글 작성 중 유사 게시글을 근거로 초안을 생성하는 RAG 기능만 평가합니다.

응답에서 다음 값을 모아 RAGAS 평가 데이터셋으로 변환합니다.

```text
user_input          = 사용자가 작성 중인 제목 + 본문
response            = RAG가 생성한 초안
retrieved_contexts  = RAG가 검색한 유사 게시글 sources
reference           = cases.json에 적은 기대 답변
```

RAGAS 점수는 다음 지표를 봅니다.

```text
faithfulness       생성 답변이 검색 context에 근거하는가
answer_relevancy   생성 답변이 사용자 입력과 관련 있는가
answer_similarity  생성 답변이 reference와 의미적으로 가까운가. 현재 CSV에는 semantic_similarity로 저장됨
context_precision  검색된 context의 순서와 유용성이 좋은가
context_recall     reference에 필요한 내용이 context에 포함되어 있는가
```

## 준비

백엔드, 프론트, DB가 켜져 있어야 합니다. 평가 자체는 백엔드 API만 사용합니다.

```powershell
cd C:\Users\cedis\week15_project\backend
.\gradlew.bat bootRun
```

별도 터미널에서 Python 가상환경을 만듭니다.

```powershell
cd C:\Users\cedis\week15_project
python -m venv eval\ragas\.venv
eval\ragas\.venv\Scripts\python.exe -m pip install -r eval\ragas\requirements.txt
```

`backend\.env`에 `OPENAI_API_KEY`가 있으면 스크립트가 자동으로 읽습니다.

## 실행

먼저 RAGAS 없이 API 수집만 확인할 수 있습니다.

```powershell
eval\ragas\.venv\Scripts\python.exe eval\ragas\run_project_alpha_ragas.py --collect-only
```

RAGAS 평가까지 실행합니다.

```powershell
eval\ragas\.venv\Scripts\python.exe eval\ragas\run_project_alpha_ragas.py
```

이미 수집한 샘플을 다시 채점할 때는 API를 다시 호출하지 않고 JSONL만 읽을 수 있습니다.

```powershell
eval\ragas\.venv\Scripts\python.exe eval\ragas\run_project_alpha_ragas.py --input-jsonl eval\ragas\output\ragas-samples-YYYYMMDD-HHMMSS.jsonl
```

결과는 Git에 올라가지 않는 `eval\ragas\output` 아래에 저장됩니다.

```text
ragas-samples-YYYYMMDD-HHMMSS.jsonl
ragas-scores-YYYYMMDD-HHMMSS.csv
```

## 케이스 추가

`cases.json`에 평가 케이스를 추가합니다.

```json
{
  "id": "case_id",
  "category": "Learning",
  "title": "질문 또는 작성 제목",
  "content": "사용자가 작성 중인 본문",
  "tags": ["RAG", "React"],
  "reference": "기대되는 답변 또는 초안의 핵심 내용"
}
```

`reference`가 좋아야 `context_recall` 같은 지표를 해석하기 쉽습니다.
