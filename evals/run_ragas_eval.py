import argparse
import json
import os
import sys
import time
import types
import warnings
from pathlib import Path

import pandas as pd
import requests
from datasets import Dataset
from langchain_openai import ChatOpenAI, OpenAIEmbeddings


def install_ragas_vertexai_shim() -> None:
    """
    Ragas 0.4.3 imports ChatVertexAI at import time.
    The currently installed langchain-community package may not ship that module.
    We only use OpenAI-based evaluation here, so a tiny shim is enough to let ragas import.
    """

    module_name = "langchain_community.chat_models.vertexai"

    if module_name in sys.modules:
        return

    shim_module = types.ModuleType(module_name)

    class ChatVertexAI:  # pragma: no cover - compatibility shim only
        pass

    shim_module.ChatVertexAI = ChatVertexAI
    sys.modules[module_name] = shim_module


install_ragas_vertexai_shim()
warnings.filterwarnings("ignore", category=DeprecationWarning)

from ragas import evaluate  # noqa: E402
from ragas.metrics import answer_relevancy, context_precision, context_recall, faithfulness  # noqa: E402


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Evaluate the board RAG endpoint with Ragas.")
    parser.add_argument("--base-url", default="http://localhost:8080", help="Backend base URL")
    parser.add_argument("--login-id", default="woojin", help="Login ID for the board API")
    parser.add_argument("--password", default="1234", help="Password for the board API")
    parser.add_argument(
        "--questions-file",
        default=str(Path("evals") / "ragas_questions.json"),
        help="Path to evaluation question JSON file",
    )
    parser.add_argument(
        "--output-dir",
        default=str(Path("evals") / "results"),
        help="Directory where CSV and JSON reports will be saved",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=0,
        help="If greater than 0, only evaluate the first N questions",
    )
    parser.add_argument(
        "--request-timeout",
        type=int,
        default=180,
        help="Timeout in seconds for each /api/rag/ask request",
    )
    return parser.parse_args()


def ensure_openai_key() -> None:
    if os.getenv("OPENAI_API_KEY", "").strip() == "":
        raise SystemExit("OPENAI_API_KEY 환경변수가 필요합니다.")


def load_questions(path: str, limit: int) -> list[dict]:
    with open(path, "r", encoding="utf-8") as file:
        questions = json.load(file)

    if limit > 0:
        return questions[:limit]

    return questions


def login(base_url: str, login_id: str, password: str) -> str:
    response = requests.post(
        f"{base_url}/api/users/login",
        headers={"Content-Type": "application/json; charset=utf-8"},
        json={"loginId": login_id, "password": password},
        timeout=60,
    )
    response.raise_for_status()
    payload = response.json()
    return payload["token"]


def call_rag(base_url: str, token: str, question: str, request_timeout: int) -> tuple[dict, float]:
    started_at = time.perf_counter()
    response = requests.post(
        f"{base_url}/api/rag/ask",
        headers={
            "Content-Type": "application/json; charset=utf-8",
            "Authorization": f"Bearer {token}",
        },
        json={"question": question},
        timeout=request_timeout,
    )
    latency_ms = (time.perf_counter() - started_at) * 1000
    response.raise_for_status()
    return response.json(), latency_ms


def build_samples(
    base_url: str,
    token: str,
    question_rows: list[dict],
    request_timeout: int,
) -> tuple[list[dict], list[dict]]:
    ragas_rows: list[dict] = []
    raw_rows: list[dict] = []

    for index, row in enumerate(question_rows, start=1):
        print(f"[{index}/{len(question_rows)}] RAG 호출 중: {row['id']} - {row['question']}")
        rag_response, latency_ms = call_rag(base_url, token, row["question"], request_timeout)
        retrieved_contexts = [reference["matchedChunkText"] for reference in rag_response.get("references", [])]
        reference_titles = [reference["title"] for reference in rag_response.get("references", [])]

        ragas_rows.append(
            {
                "user_input": row["question"],
                "response": rag_response.get("answer", ""),
                "retrieved_contexts": retrieved_contexts,
                "reference": row["reference"],
            }
        )

        raw_rows.append(
            {
                "id": row["id"],
                "question": row["question"],
                "reference_answer": row["reference"],
                "rag_answer": rag_response.get("answer", ""),
                "total_matches": rag_response.get("totalMatches", 0),
                "reference_titles": " | ".join(reference_titles),
                "latency_ms": round(latency_ms, 2),
            }
        )
        print(f"  완료: matches={rag_response.get('totalMatches', 0)}, latency_ms={round(latency_ms, 2)}")

    return ragas_rows, raw_rows


def run_evaluation(samples: list[dict]) -> pd.DataFrame:
    dataset = Dataset.from_list(samples)

    evaluator_llm = ChatOpenAI(model="gpt-4.1-mini", temperature=0)
    evaluator_embeddings = OpenAIEmbeddings(model="text-embedding-3-small")

    result = evaluate(
        dataset=dataset,
        metrics=[faithfulness, answer_relevancy, context_precision, context_recall],
        llm=evaluator_llm,
        embeddings=evaluator_embeddings,
        raise_exceptions=True,
        show_progress=True,
    )

    return result.to_pandas()


def build_summary(raw_df: pd.DataFrame, score_df: pd.DataFrame) -> dict:
    metric_columns = [
        column
        for column in ["faithfulness", "answer_relevancy", "context_precision", "context_recall"]
        if column in score_df.columns
    ]

    metric_means = {
        column: round(float(score_df[column].mean()), 4)
        for column in metric_columns
    }

    return {
        "sample_count": int(len(raw_df)),
        "average_latency_ms": round(float(raw_df["latency_ms"].mean()), 2),
        "max_latency_ms": round(float(raw_df["latency_ms"].max()), 2),
        "min_latency_ms": round(float(raw_df["latency_ms"].min()), 2),
        "average_total_matches": round(float(raw_df["total_matches"].mean()), 2),
        "metric_means": metric_means,
    }


def main() -> None:
    args = parse_args()
    ensure_openai_key()

    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    question_rows = load_questions(args.questions_file, args.limit)
    token = login(args.base_url, args.login_id, args.password)
    ragas_rows, raw_rows = build_samples(
        args.base_url,
        token,
        question_rows,
        args.request_timeout,
    )

    raw_df = pd.DataFrame(raw_rows)
    score_df = run_evaluation(ragas_rows)
    merged_df = pd.concat([raw_df.reset_index(drop=True), score_df.reset_index(drop=True)], axis=1)

    summary = build_summary(raw_df, score_df)

    timestamp = time.strftime("%Y%m%d-%H%M%S")
    csv_path = output_dir / f"ragas-results-{timestamp}.csv"
    json_path = output_dir / f"ragas-summary-{timestamp}.json"

    merged_df.to_csv(csv_path, index=False, encoding="utf-8-sig")
    with open(json_path, "w", encoding="utf-8") as file:
        json.dump(summary, file, ensure_ascii=False, indent=2)

    print("Ragas 평가 완료")
    print(f"CSV 결과: {csv_path}")
    print(f"요약 JSON: {json_path}")
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
