from __future__ import annotations

import argparse
import csv
import json
import os
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import requests


PROJECT_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_CASES_PATH = Path(__file__).with_name("cases.json")
DEFAULT_OUTPUT_DIR = Path(__file__).with_name("output")


@dataclass(frozen=True)
class EvaluationCase:
    case_id: str
    category: str
    title: str
    content: str
    tags: list[str]
    reference: str

    @property
    def user_input(self) -> str:
        return f"{self.title}\n\n{self.content}".strip()


def main() -> None:
    load_env_file(PROJECT_ROOT / "backend" / ".env")
    load_env_file(PROJECT_ROOT / ".env")

    parser = argparse.ArgumentParser(
        description="Collect Project Alpha RAG outputs and evaluate them with RAGAS."
    )
    parser.add_argument("--api-base-url", default=os.getenv("PROJECT_ALPHA_API_BASE_URL", "http://localhost:8080"))
    parser.add_argument("--username", default=os.getenv("PROJECT_ALPHA_EVAL_USERNAME", "practice1"))
    parser.add_argument("--password", default=os.getenv("PROJECT_ALPHA_EVAL_PASSWORD", "alpha1234"))
    parser.add_argument("--cases", type=Path, default=DEFAULT_CASES_PATH)
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR)
    parser.add_argument("--limit", type=int, default=5)
    parser.add_argument(
        "--input-jsonl",
        type=Path,
        help="Score an existing samples JSONL file instead of calling the Project Alpha API again.",
    )
    parser.add_argument(
        "--collect-only",
        action="store_true",
        help="Only call the Project Alpha API and write JSONL. Skip RAGAS scoring.",
    )
    args = parser.parse_args()

    args.output_dir.mkdir(parents=True, exist_ok=True)

    timestamp = time.strftime("%Y%m%d-%H%M%S")
    if args.input_jsonl:
        samples = read_jsonl(args.input_jsonl)
        print(f"Loaded {len(samples)} samples: {args.input_jsonl}")
    else:
        cases = load_cases(args.cases)
        token = login(args.api_base_url, args.username, args.password)
        samples = [collect_sample(args.api_base_url, token, case, args.limit) for case in cases]

        jsonl_path = args.output_dir / f"ragas-samples-{timestamp}.jsonl"
        write_jsonl(jsonl_path, samples)
        print(f"Collected {len(samples)} samples: {jsonl_path}")

    if args.collect_only:
        print("Skipped RAGAS scoring because --collect-only was provided.")
        return

    scores_path = args.output_dir / f"ragas-scores-{timestamp}.csv"
    evaluate_with_ragas(samples, scores_path)
    print(f"RAGAS scores: {scores_path}")


def load_env_file(path: Path) -> None:
    if not path.exists():
        return

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue

        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")

        if key and key not in os.environ:
            os.environ[key] = value


def load_cases(path: Path) -> list[EvaluationCase]:
    raw_cases = json.loads(path.read_text(encoding="utf-8"))
    cases: list[EvaluationCase] = []

    for raw_case in raw_cases:
        cases.append(
            EvaluationCase(
                case_id=str(raw_case["id"]),
                category=str(raw_case["category"]),
                title=str(raw_case["title"]),
                content=str(raw_case["content"]),
                tags=[str(tag) for tag in raw_case.get("tags", [])],
                reference=str(raw_case["reference"]),
            )
        )

    return cases


def login(api_base_url: str, username: str, password: str) -> str:
    response = requests.post(
        f"{api_base_url}/api/auth/login",
        json={"username": username, "password": password},
        timeout=15,
    )

    if response.status_code == 401:
        signup_response = requests.post(
            f"{api_base_url}/api/auth/signup",
            json={"username": username, "password": password},
            timeout=15,
        )
        signup_response.raise_for_status()
        response = requests.post(
            f"{api_base_url}/api/auth/login",
            json={"username": username, "password": password},
            timeout=15,
        )

    response.raise_for_status()
    token = response.json().get("token")
    if not token:
        raise RuntimeError("Login succeeded but token was missing.")

    return str(token)


def collect_sample(api_base_url: str, token: str, case: EvaluationCase, limit: int) -> dict[str, Any]:
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
    }
    request_body = {
        "category": case.category,
        "title": case.title,
        "content": case.content,
        "tags": case.tags,
        "excludedPostId": None,
        "limit": limit,
    }
    response = requests.post(
        f"{api_base_url}/api/ai/draft",
        headers=headers,
        json=request_body,
        timeout=90,
    )
    response.raise_for_status()
    draft_result = response.json()
    sources = draft_result.get("sources", [])

    retrieved_contexts = [
        format_source_context(source)
        for source in sources
        if str(source.get("content", "")).strip()
    ]

    return {
        "case_id": case.case_id,
        "user_input": case.user_input,
        "response": str(draft_result.get("draft", "")),
        "retrieved_contexts": retrieved_contexts,
        "reference": case.reference,
        "source_titles": [str(source.get("title", "")) for source in sources],
        "source_scores": [float(source.get("score", 0)) for source in sources],
        "source_matched_terms": [read_matched_terms(source) for source in sources],
    }


def format_source_context(source: dict[str, Any]) -> str:
    tags = source.get("tags", [])
    tag_text = ", ".join(str(tag) for tag in tags) if isinstance(tags, list) else ""

    return "\n".join(
        part
        for part in [
            f"Title: {source.get('title', '')}",
            f"Category: {source.get('category', '')}",
            f"Tags: {tag_text}",
            f"Content: {source.get('content', '')}",
        ]
        if part.strip()
    )


def read_matched_terms(source: dict[str, Any]) -> list[str]:
    score_breakdown = source.get("scoreBreakdown", {})
    if not isinstance(score_breakdown, dict):
        return []

    matched_terms = score_breakdown.get("matchedTerms", [])
    if not isinstance(matched_terms, list):
        return []

    return [str(term) for term in matched_terms]


def write_jsonl(path: Path, rows: list[dict[str, Any]]) -> None:
    with path.open("w", encoding="utf-8") as output_file:
        for row in rows:
            output_file.write(json.dumps(row, ensure_ascii=False) + "\n")


def read_jsonl(path: Path) -> list[dict[str, Any]]:
    with path.open("r", encoding="utf-8") as input_file:
        return [json.loads(line) for line in input_file if line.strip()]


def evaluate_with_ragas(samples: list[dict[str, Any]], scores_path: Path) -> None:
    try:
        from datasets import Dataset
        from ragas import evaluate
        from ragas.metrics import (
            answer_relevancy,
            answer_similarity,
            context_precision,
            context_recall,
            faithfulness,
        )
    except ImportError as exception:
        raise RuntimeError(
            "RAGAS dependencies are not installed. Run: "
            "python -m pip install -r eval/ragas/requirements.txt"
        ) from exception

    dataset = Dataset.from_list(
        [
            {
                "user_input": read_sample_value(sample, "user_input", "question"),
                "response": read_sample_value(sample, "response", "answer"),
                "retrieved_contexts": read_sample_value(sample, "retrieved_contexts", "contexts"),
                "reference": read_sample_value(sample, "reference", "ground_truth"),
            }
            for sample in samples
        ]
    )

    result = evaluate(
        dataset,
        metrics=[
            faithfulness,
            answer_relevancy,
            answer_similarity,
            context_precision,
            context_recall,
        ],
    )
    dataframe = result.to_pandas()
    dataframe.insert(0, "case_id", [sample["case_id"] for sample in samples])
    dataframe.insert(1, "source_titles", [format_source_titles(sample) for sample in samples])
    dataframe.to_csv(scores_path, index=False, encoding="utf-8-sig")
    print_score_summary(dataframe)


def read_sample_value(sample: dict[str, Any], primary_key: str, legacy_key: str) -> Any:
    if primary_key in sample:
        return sample[primary_key]

    return sample[legacy_key]


def format_source_titles(sample: dict[str, Any]) -> str:
    titles = sample.get("source_titles", [])
    scores = sample.get("source_scores", [])
    matched_terms = sample.get("source_matched_terms", [])

    if not isinstance(titles, list):
        return ""

    formatted_sources = []
    for index, title in enumerate(titles):
        score = scores[index] if isinstance(scores, list) and index < len(scores) else 0.0
        terms = matched_terms[index] if isinstance(matched_terms, list) and index < len(matched_terms) else []
        term_text = ", ".join(str(term) for term in terms) if isinstance(terms, list) else ""
        formatted_sources.append(f"{score:.3f} {title} [{term_text}]")

    return " | ".join(formatted_sources)


def print_score_summary(dataframe: Any) -> None:
    metric_columns = [
        column
        for column in [
            "faithfulness",
            "answer_relevancy",
            "answer_similarity",
            "semantic_similarity",
            "context_precision",
            "context_recall",
        ]
        if column in dataframe.columns
    ]
    print(dataframe[["case_id", *metric_columns]].to_string(index=False))


if __name__ == "__main__":
    try:
        main()
    except Exception as exception:
        print(f"RAGAS evaluation failed: {exception}", file=sys.stderr)
        raise
