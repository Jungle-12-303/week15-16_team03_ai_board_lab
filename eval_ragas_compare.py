#!/usr/bin/env python3
import argparse
import json
import os
import signal
import subprocess
import sys
import threading
import time
import types
import urllib.error
import urllib.parse
import urllib.request
from collections import deque
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parent
BACKEND_DIR = ROOT_DIR / "backend"

EVAL_EMAIL = "ragas-eval@example.com"
EVAL_PASSWORD = "password123"
EVAL_NICKNAME = "ragas-eval"

SEED_POSTS = [
    {
        "title": "RAGAS 평가용 v2 - 인증과 Spring Security 종합 노트",
        "content": (
            "1. 로그인 기본 흐름\n"
            "로그인은 사용자가 이메일과 비밀번호를 서버로 보내는 것에서 시작한다. "
            "서버는 사용자 정보를 조회하고 비밀번호 해시를 검증한다. "
            "검증이 성공하면 JWT를 발급해서 프론트엔드에 반환한다. "
            "프론트엔드는 JWT를 localStorage에 저장하고 이후 API 요청마다 Authorization 헤더에 Bearer 토큰을 붙인다. "
            "백엔드는 JWT 필터에서 토큰을 검증하고 SecurityContext에 인증 정보를 저장한다.\n\n"
            "2. JWT 필터의 위치와 책임\n"
            "JWT 필터는 UsernamePasswordAuthenticationFilter보다 앞에서 실행된다. "
            "이 필터는 Authorization 헤더가 Bearer 형식인지 확인하고, 토큰의 서명과 만료 시간을 검증한다. "
            "토큰이 유효하면 userId를 꺼내 사용자 정보를 조회하고 인증 객체를 만들어 SecurityContext에 넣는다. "
            "이 과정이 끝난 뒤 컨트롤러와 서비스는 현재 사용자를 신뢰하고 사용할 수 있다.\n\n"
            "3. Refresh Token 재발급 규칙\n"
            "Refresh Token은 모든 API 요청마다 새로 발급하지 않는다. "
            "Access Token이 만료되었고 Refresh Token이 아직 유효할 때만 재발급 API에서 새 Access Token을 만든다. "
            "보안을 높이려면 Refresh Token도 함께 회전시키고, 이전 Refresh Token은 서버 저장소에서 폐기한다. "
            "재발급에 실패하면 사용자는 다시 로그인해야 한다.\n\n"
            "4. CORS preflight와 인증의 관계\n"
            "브라우저는 Authorization 헤더가 붙은 요청을 보내기 전에 OPTIONS preflight 요청을 보낼 수 있다. "
            "이 OPTIONS 요청은 실제 API 요청이 아니라 브라우저가 서버의 허용 정책을 확인하는 절차다. "
            "따라서 CORS 설정에서 허용 origin, method, header를 정확히 열어야 하고, 인증 필터가 preflight를 불필요하게 막지 않도록 해야 한다."
        ),
        "tags": ["spring", "security", "jwt"],
    },
    {
        "title": "RAGAS 평가용 v2 - RAG 인덱싱과 pgvector 운영 노트",
        "content": (
            "1. 문서 로드와 청킹\n"
            "게시판 RAG의 원본 데이터는 posts 테이블에 저장된 제목과 본문이다. "
            "RagService는 이 값을 Spring AI Document로 만들고, splitter가 본문을 여러 청크로 나눈다. "
            "짧은 글은 하나의 청크가 될 수 있지만 긴 글은 검색 정확도를 위해 여러 청크로 나누는 것이 좋다. "
            "청크가 너무 크면 질문과 관련 없는 내용이 같이 들어가고, 너무 작으면 의미가 끊긴다.\n\n"
            "2. pgvector와 vector_store의 역할\n"
            "pgvector는 PostgreSQL에서 벡터 타입과 벡터 유사도 검색을 사용할 수 있게 해주는 확장이다. "
            "Spring AI VectorStore는 각 청크의 embedding, 원문 일부, metadata를 PostgreSQL의 vector_store 테이블에 저장한다. "
            "metadata에는 postId, title, authorNickname 같은 값이 들어가서 검색 결과를 다시 게시글 링크로 연결할 수 있다. "
            "사용자 질문도 embedding으로 바뀌고, vector_store는 코사인 거리 기준으로 가까운 청크를 찾는다.\n\n"
            "3. 전체 재인덱싱이 필요한 경우\n"
            "전체 재인덱싱은 기존 vector_store의 게시글 벡터를 삭제하고 posts 테이블의 모든 게시글을 다시 청킹, 임베딩, 저장하는 작업이다. "
            "청킹 전략을 TokenTextSplitter에서 문단 기반 splitter로 바꿨거나, embedding 모델 차원을 바꿨거나, 과거 인덱스가 꼬였을 때 필요하다. "
            "평소 게시글 생성과 수정은 해당 게시글만 다시 인덱싱하면 되고, 질문할 때마다 전체 재인덱싱을 하면 안 된다.\n\n"
            "4. 질문 검색과 답변 생성\n"
            "사용자가 질문하면 질문 문장을 embedding으로 바꾼다. "
            "그 다음 VectorStore similaritySearch가 topK와 similarityThreshold 조건에 맞는 청크를 검색한다. "
            "LLM은 검색된 청크만 근거로 답변해야 하며, 검색 결과가 없으면 모른다고 답하는 편이 안전하다."
        ),
        "tags": ["rag", "pgvector", "embedding"],
    },
    {
        "title": "RAGAS 평가용 v2 - Promise와 이벤트 루프 종합 정리",
        "content": (
            "1. 콜백 함수 연결 방식\n"
            "자바스크립트에서 콜백 함수는 다른 함수의 인자로 전달된 뒤 나중에 호출되는 함수다. "
            "예를 들어 first(second)를 호출하면 first 함수 안의 파라미터가 second 함수를 가리키고, "
            "파라미터()를 실행하는 순간 실제로 second()가 호출된다. "
            "이 구조를 이해하면 이벤트 리스너와 비동기 API의 동작을 설명할 수 있다.\n\n"
            "2. Promise의 resolve와 reject\n"
            "Promise는 비동기 작업의 성공과 실패를 표현하는 객체다. "
            "resolve는 작업이 성공했을 때 호출되고 reject는 작업이 실패했을 때 호출된다. "
            "Promise 생성자 안의 작업 함수는 resolve와 reject를 인자로 받고, 성공 조건에서는 resolve(value)를 호출한다.\n\n"
            "3. then과 catch 실행 시점\n"
            "then은 resolve로 전달된 성공 결과를 받아 처리한다. "
            "catch는 reject로 전달된 실패 이유를 받아 처리한다. "
            "중요한 점은 then 콜백이 즉시 동기적으로 실행되는 것이 아니라 마이크로태스크 큐에 등록된 뒤 현재 콜스택이 비었을 때 실행된다는 것이다. "
            "그래서 console.log와 Promise.then을 같이 쓰면 동기 로그가 먼저 찍히고 then 콜백이 나중에 실행된다.\n\n"
            "4. 이벤트 루프와 태스크 큐\n"
            "브라우저와 Node.js는 콜스택이 비었을 때 큐에 쌓인 작업을 꺼내 실행한다. "
            "마이크로태스크 큐는 일반 태스크 큐보다 먼저 처리되며 Promise.then 콜백이 여기에 들어간다. "
            "setTimeout 콜백은 일반 태스크 큐에 들어가므로 같은 코드 안에서는 Promise.then보다 늦게 실행될 수 있다."
        ),
        "tags": ["javascript", "promise"],
    },
    {
        "title": "RAGAS 평가용 v2 - 금리와 기술주 투자 토론",
        "content": (
            "1. 금리 상승과 기술주 밸류에이션\n"
            "금리가 상승하면 미래 이익을 현재 가치로 할인하는 비율이 커진다. "
            "이 때문에 먼 미래의 성장 기대가 가격에 많이 반영된 기술주와 성장주는 부담을 받을 수 있다. "
            "특히 나스닥은 대형 기술주 비중이 높아서 금리 전망 변화에 민감하게 반응하는 경우가 많다.\n\n"
            "2. 나스닥과 국내 반도체 투자 심리\n"
            "나스닥 흐름은 국내 반도체 종목의 투자 심리에도 영향을 줄 수 있다. "
            "미국 기술주가 강하면 위험자산 선호가 살아나고, 국내 반도체 대형주에도 긍정적인 분위기가 생길 수 있다. "
            "반대로 나스닥이 급락하면 삼성전자와 SK하이닉스 같은 종목도 단기적으로 부담을 받을 수 있다.\n\n"
            "3. 삼성전자 판단 기준\n"
            "삼성전자는 단순히 코스피 지수만 보고 판단하기 어렵다. "
            "반도체 업황, 메모리 가격, 환율, 글로벌 스마트폰과 서버 수요, 외국인 수급을 함께 봐야 한다. "
            "금리가 높아도 반도체 사이클이 개선되면 주가가 버틸 수 있고, 업황이 나쁘면 낮은 금리 환경에서도 약할 수 있다.\n\n"
            "4. 게시판 토론에서 주의할 점\n"
            "투자 게시글은 특정 종목 매수를 권유하는 방식보다 근거와 리스크를 함께 정리하는 방식이 좋다. "
            "금리, 나스닥, 삼성전자 전망을 말할 때도 단정적인 표현보다는 어떤 조건에서 긍정적이고 어떤 조건에서 위험한지 나누어 설명해야 한다."
        ),
        "tags": ["stock", "finance"],
    },
]

TEST_CASES = [
    {
        "question": "Refresh Token은 언제 재발급돼?",
        "reference": (
            "Refresh Token은 모든 API 요청마다 재발급하지 않는다. "
            "Access Token이 만료되었고 Refresh Token이 아직 유효할 때 재발급 API에서 새 Access Token을 만든다. "
            "보안을 높이려면 Refresh Token도 회전시키고 기존 Refresh Token은 폐기한다."
        ),
    },
    {
        "question": "RAG에서 전체 재인덱싱은 언제 해야 해?",
        "reference": (
            "전체 재인덱싱은 기존 vector_store의 게시글 벡터를 삭제하고 모든 게시글을 다시 청킹, 임베딩, 저장하는 작업이다. "
            "청킹 전략을 바꿨거나 embedding 모델 차원을 바꿨거나 과거 인덱스가 꼬였을 때 필요하다. "
            "일반적인 게시글 생성과 수정은 해당 게시글만 다시 인덱싱하면 된다."
        ),
    },
    {
        "question": "Promise의 then 콜백은 언제 실행돼?",
        "reference": (
            "then은 resolve가 전달한 성공 결과를 처리하지만 즉시 동기적으로 실행되지 않는다. "
            "then 콜백은 마이크로태스크 큐에 등록되고 현재 콜스택이 비었을 때 실행된다. "
            "따라서 동기 console.log가 먼저 실행되고 then 콜백이 나중에 실행될 수 있다."
        ),
    },
    {
        "question": "금리 상승이 나스닥과 삼성전자에 주는 영향은 뭐야?",
        "reference": (
            "금리가 상승하면 미래 이익의 할인율이 커져 기술주와 성장주 밸류에이션에 부담을 줄 수 있다. "
            "나스닥은 기술주 비중이 높아 금리 변화에 민감하고, 나스닥 흐름은 국내 반도체 투자 심리에도 영향을 줄 수 있다. "
            "삼성전자는 금리뿐 아니라 반도체 업황, 메모리 가격, 환율, 글로벌 수요와 외국인 수급을 함께 봐야 한다."
        ),
    },
]


def load_dotenv():
    dotenv = ROOT_DIR / ".env"
    if not dotenv.exists():
        return

    for line in dotenv.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue

        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip("'").strip('"')
        os.environ.setdefault(key, value)


def request_json(method, base_url, path, data=None, token=None, timeout=30):
    body = None
    headers = {"Accept": "application/json"}
    if data is not None:
        body = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"

    request = urllib.request.Request(
        base_url + path,
        data=body,
        headers=headers,
        method=method,
    )

    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            raw = response.read().decode("utf-8")
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as error:
        raw = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {path} failed with HTTP {error.code}: {raw}") from error
    except urllib.error.URLError as error:
        raise RuntimeError(f"{method} {path} failed: {error}") from error


def get_json(base_url, path, token=None, timeout=30):
    return request_json("GET", base_url, path, token=token, timeout=timeout)


def post_json(base_url, path, data=None, token=None, timeout=30):
    return request_json("POST", base_url, path, data=data or {}, token=token, timeout=timeout)


class BackendProcess:
    def __init__(self, splitter, port, timeout):
        self.splitter = splitter
        self.port = port
        self.timeout = timeout
        self.process = None
        self.logs = deque(maxlen=120)
        self.reader_thread = None

    @property
    def base_url(self):
        return f"http://localhost:{self.port}"

    def __enter__(self):
        env = os.environ.copy()
        env["SERVER_PORT"] = str(self.port)
        env["RAG_SPLITTER"] = self.splitter
        env.setdefault("RAG_TOP_K", "12")
        env.setdefault("RAG_SIMILARITY_THRESHOLD", "0.0")

        self.process = subprocess.Popen(
            ["./gradlew", "bootRun", "--quiet"],
            cwd=BACKEND_DIR,
            env=env,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
            preexec_fn=os.setsid,
        )
        self.reader_thread = threading.Thread(target=self._read_logs, daemon=True)
        self.reader_thread.start()
        self._wait_until_ready()
        return self

    def __exit__(self, exc_type, exc, traceback):
        if self.process and self.process.poll() is None:
            os.killpg(os.getpgid(self.process.pid), signal.SIGTERM)
            try:
                self.process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                os.killpg(os.getpgid(self.process.pid), signal.SIGKILL)
                self.process.wait(timeout=5)

    def _read_logs(self):
        if not self.process or not self.process.stdout:
            return

        for line in self.process.stdout:
            self.logs.append(line.rstrip())

    def _wait_until_ready(self):
        deadline = time.time() + self.timeout
        path = "/api/posts?size=1"

        while time.time() < deadline:
            if self.process and self.process.poll() is not None:
                raise RuntimeError(
                    f"Backend stopped while starting with RAG_SPLITTER={self.splitter}.\n"
                    + "\n".join(self.logs)
                )

            try:
                get_json(self.base_url, path, timeout=3)
                return
            except Exception:
                time.sleep(2)

        raise RuntimeError(
            f"Backend did not start within {self.timeout}s with RAG_SPLITTER={self.splitter}.\n"
            + "\n".join(self.logs)
        )


def login_or_signup(base_url):
    login_request = {"email": EVAL_EMAIL, "password": EVAL_PASSWORD}
    try:
        response = post_json(base_url, "/api/auth/login", login_request)
        return response["token"]
    except RuntimeError:
        signup_request = {
            "email": EVAL_EMAIL,
            "password": EVAL_PASSWORD,
            "nickname": EVAL_NICKNAME,
        }
        response = post_json(base_url, "/api/auth/signup", signup_request)
        return response["token"]


def ensure_seed_posts(base_url, token):
    for post in SEED_POSTS:
        query = urllib.parse.urlencode({"keyword": post["title"], "size": 50})
        result = get_json(base_url, f"/api/posts?{query}")
        existing = next((item for item in result.get("content", []) if item.get("title") == post["title"]), None)
        if existing:
            try:
                request_json("PUT", base_url, f"/api/posts/{existing['id']}", post, token=token, timeout=60)
                continue
            except RuntimeError:
                continue

        post_json(base_url, "/api/posts", post, token=token, timeout=60)


def reindex(base_url, token):
    result = post_json(base_url, "/api/ai/rag/reindex", token=token, timeout=180)
    return result.get("indexedPosts", 0)


def collect_rag_outputs(base_url, token):
    rows = []

    for case in TEST_CASES:
        response = post_json(
            base_url,
            "/api/ai/rag/chat",
            {"message": case["question"]},
            token=token,
            timeout=90,
        )
        sources = response.get("sources", [])
        contexts = [
            f"Title: {source.get('title', '')}\nContent: {source.get('contentPreview', '')}"
            for source in sources
        ]
        rows.append(
            {
                "user_input": case["question"],
                "response": response.get("summary", ""),
                "retrieved_contexts": contexts,
                "reference": case["reference"],
                "source_titles": [source.get("title", "") for source in sources],
            }
        )

    return rows


def require_ragas():
    install_vertexai_import_shim()

    try:
        from openai import AsyncOpenAI
        from ragas.llms import llm_factory
        from ragas.metrics.collections import ContextPrecision, ContextRecall, Faithfulness
    except ImportError as error:
        raise RuntimeError(
            "RAGAS import failed. First try:\n"
            "python3 -m pip install -U ragas openai langchain-community\n\n"
            f"Original import error: {error}"
        ) from error

    return AsyncOpenAI, llm_factory, ContextPrecision, ContextRecall, Faithfulness


def install_vertexai_import_shim():
    module_name = "langchain_community.chat_models.vertexai"

    if module_name in sys.modules:
        return

    module = types.ModuleType(module_name)
    module.ChatVertexAI = type("ChatVertexAI", (), {})
    sys.modules[module_name] = module


def score_with_ragas(rows):
    AsyncOpenAI, llm_factory, ContextPrecision, ContextRecall, Faithfulness = require_ragas()

    client = AsyncOpenAI(api_key=os.environ.get("OPENAI_API_KEY"))
    model = os.environ.get("RAGAS_EVAL_MODEL", "gpt-4o-mini")
    llm = llm_factory(model, client=client)

    scorers = {
        "faithfulness": Faithfulness(llm=llm),
        "context_precision": ContextPrecision(llm=llm),
        "context_recall": ContextRecall(llm=llm),
    }
    scores = {name: [] for name in scorers}

    for row in rows:
        if not row["retrieved_contexts"]:
            for name in scores:
                scores[name].append(0.0)
            continue

        scores["faithfulness"].append(
            score_value(
                scorers["faithfulness"].score(
                    user_input=row["user_input"],
                    response=row["response"],
                    retrieved_contexts=row["retrieved_contexts"],
                )
            )
        )
        scores["context_precision"].append(
            score_value(
                scorers["context_precision"].score(
                    user_input=row["user_input"],
                    reference=row["reference"],
                    retrieved_contexts=row["retrieved_contexts"],
                )
            )
        )
        scores["context_recall"].append(
            score_value(
                scorers["context_recall"].score(
                    user_input=row["user_input"],
                    reference=row["reference"],
                    retrieved_contexts=row["retrieved_contexts"],
                )
            )
        )

    return {name: average(values) for name, values in scores.items()}


def score_value(result):
    return float(getattr(result, "value", result))


def average(values):
    if not values:
        return 0.0
    return sum(values) / len(values)


def print_table(headers, rows):
    widths = [len(header) for header in headers]
    for row in rows:
        for index, value in enumerate(row):
            widths[index] = max(widths[index], len(str(value)))

    def format_row(row):
        return " | ".join(str(value).ljust(widths[index]) for index, value in enumerate(row))

    print(format_row(headers))
    print("-+-".join("-" * width for width in widths))
    for row in rows:
        print(format_row(row))


def run_splitter(splitter, port, timeout):
    print(f"\n=== Running {splitter} splitter on port {port} ===")
    with BackendProcess(splitter, port, timeout) as backend:
        try:
            token = login_or_signup(backend.base_url)
            ensure_seed_posts(backend.base_url, token)
            indexed_posts = reindex(backend.base_url, token)
            print(f"Reindexed posts: {indexed_posts}")

            rows = collect_rag_outputs(backend.base_url, token)
            for row in rows:
                titles = ", ".join(row["source_titles"]) or "(no sources)"
                print(f"- Q: {row['user_input']}")
                print(f"  sources: {titles}")

            scores = score_with_ragas(rows)
            print_table(
                ["metric", splitter],
                [[name, f"{value:.4f}"] for name, value in scores.items()],
            )
            return scores
        except Exception as error:
            print("\n--- Backend logs before failure ---", file=sys.stderr)
            print("\n".join(backend.logs), file=sys.stderr)
            raise error


def main():
    parser = argparse.ArgumentParser(description="Compare RAG splitters with RAGAS.")
    parser.add_argument("--port", type=int, default=18080)
    parser.add_argument("--timeout", type=int, default=120)
    args = parser.parse_args()

    load_dotenv()

    if not os.environ.get("OPENAI_API_KEY"):
        raise RuntimeError("OPENAI_API_KEY is required in your shell env or .env file.")

    summaries = {}
    for index, splitter in enumerate(["token", "rule"]):
        summaries[splitter] = run_splitter(splitter, args.port + index, args.timeout)

    print("\n=== Comparison summary ===")
    metric_names = sorted(set(summaries["token"]) | set(summaries["rule"]))
    rows = []
    for metric in metric_names:
        token_score = summaries["token"].get(metric, 0.0)
        rule_score = summaries["rule"].get(metric, 0.0)
        if rule_score > token_score:
            winner = "rule"
        elif token_score > rule_score:
            winner = "token"
        else:
            winner = "tie"
        rows.append([metric, f"{token_score:.4f}", f"{rule_score:.4f}", winner])

    print_table(["metric", "token", "rule", "winner"], rows)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nInterrupted.", file=sys.stderr)
        sys.exit(130)
    except Exception as error:
        print(f"\nERROR: {error}", file=sys.stderr)
        sys.exit(1)
