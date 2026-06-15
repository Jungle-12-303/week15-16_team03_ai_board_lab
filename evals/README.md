# Ragas Evaluation

이 폴더는 게시판 RAG 기능의 품질 평가용입니다.

## 용도

- `faithfulness`: 답변이 검색된 문맥에 근거하는지
- `answer_relevancy`: 답변이 질문과 관련 있는지
- `context_precision`: 검색된 문맥이 질문 기준으로 적절한지
- `context_recall`: 검색된 문맥이 기준 답변을 뒷받침하는 데 충분한지

## 준비

1. 백엔드 실행
2. `OPENAI_API_KEY` 설정
3. 게시글 임베딩 갱신 완료

## 설치

가상환경 예시:

```powershell
python -m venv .venv-ragas
.\.venv-ragas\Scripts\pip install -r evals\requirements-ragas.txt
```

## 실행

기본 실행:

```powershell
.\.venv-ragas\Scripts\python evals\run_ragas_eval.py
```

배포 서버 대상 실행 예시:

```powershell
.\.venv-ragas\Scripts\python evals\run_ragas_eval.py --base-url https://ai-board-backend-htf1.onrender.com
```

질문 개수 제한 예시:

```powershell
.\.venv-ragas\Scripts\python evals\run_ragas_eval.py --limit 5
```

## 결과

- `evals/results/ragas-results-YYYYMMDD-HHMMSS.csv`
- `evals/results/ragas-summary-YYYYMMDD-HHMMSS.json`

## 주의

- 이 평가는 `부하 테스트`가 아닙니다.
- 응답 품질 평가와 평균 지연 시간 확인용입니다.
- 순수 트래픽/동시성 테스트는 `k6`, `JMeter`, `Locust` 같은 도구가 더 적합합니다.
