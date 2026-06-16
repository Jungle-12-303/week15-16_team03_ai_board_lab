import argparse
import json
import random
import sys
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any


DEFAULT_BASE_URL = "https://ai-board-backend-htf1.onrender.com"
DEFAULT_PASSWORD = "seedpass1234"
DEFAULT_STATE_FILE = Path("C:/tmp/ai-board-seed-state.json")

SHORT_POST_WEIGHT = 0.6
MEDIUM_POST_WEIGHT = 0.3
LONG_POST_WEIGHT = 0.1


TOPICS = [
    {
        "name": "spring",
        "display": "Spring Boot",
        "tags": ["spring", "jpa", "api", "backend"],
        "titles": [
            "Spring Boot 게시글 API 구현 기록",
            "JPA 저장 흐름을 정리한 메모",
            "컨트롤러와 서비스 분리 이유 정리",
            "게시글 CRUD를 만들면서 이해한 백엔드 흐름",
        ],
        "sentences": [
            "게시글 목록과 상세 조회를 구현하면서 요청이 컨트롤러에서 서비스로 넘어가는 흐름을 다시 정리했다.",
            "JPA 엔티티를 만들고 Repository를 연결하니 SQL을 직접 많이 작성하지 않아도 기본 CRUD가 가능했다.",
            "Service 계층에서 권한 검사와 비즈니스 로직을 모아두는 구조가 유지보수에 더 유리하다고 느꼈다.",
            "생성, 수정, 삭제마다 예외 처리를 명확히 나누는 것이 이후 디버깅 시간을 줄여준다.",
        ],
    },
    {
        "name": "react",
        "display": "React",
        "tags": ["react", "typescript", "frontend", "vite"],
        "titles": [
            "React 상태 관리 기초 정리",
            "게시글 목록 화면을 구성한 과정",
            "useEffect와 fetch를 연결한 실습 기록",
            "TypeScript로 게시글 타입을 정의한 이유",
        ],
        "sentences": [
            "프론트에서는 게시글 목록과 상세를 한 화면에 모두 두지 않고 상태에 따라 분리해서 보여주는 방식으로 수정했다.",
            "useState로 입력값을 관리하고 useEffect로 데이터를 불러오니 화면 흐름이 훨씬 명확해졌다.",
            "타입을 분리해두면 백엔드 응답 구조가 바뀌었을 때 어디를 수정해야 하는지 빠르게 찾을 수 있다.",
            "검색어와 페이지 번호를 상태로 분리해두니 페이지네이션 동작을 예측하기 쉬웠다.",
        ],
    },
    {
        "name": "postgres",
        "display": "PostgreSQL",
        "tags": ["postgresql", "database", "sql", "schema"],
        "titles": [
            "PostgreSQL 연결 설정 정리",
            "게시판 테이블 구조를 단순하게 시작한 이유",
            "댓글과 게시글 관계를 DB 기준으로 다시 이해하기",
            "JPA update 모드와 테이블 생성 흐름 확인",
        ],
        "sentences": [
            "처음에는 posts 테이블 하나만 두고 시작해서 전체 흐름을 이해하는 데 집중했다.",
            "외래키를 쓰면 댓글이 어떤 게시글에 속하는지 DB 차원에서 명확하게 관리할 수 있다.",
            "개발 초기에는 ddl-auto update가 편하지만 배포 환경에서는 스키마 관리 전략을 따로 생각해야 한다.",
            "pgAdmin으로 실제 데이터가 어떻게 저장되는지 확인하는 과정이 구조를 이해하는 데 도움이 됐다.",
        ],
    },
    {
        "name": "jwt",
        "display": "JWT 인증",
        "tags": ["jwt", "auth", "login", "security"],
        "titles": [
            "JWT 로그인 흐름 정리",
            "Bearer 토큰을 헤더에 넣는 이유",
            "작성자 권한 검사를 추가한 기록",
            "로그인 후 내 글만 수정 가능하게 만든 과정",
        ],
        "sentences": [
            "로그인에 성공하면 토큰을 발급하고 이후 요청에서는 Authorization 헤더로 전달하도록 구성했다.",
            "게시글 수정과 삭제는 ownerLoginId와 현재 로그인 사용자를 비교해서 권한을 확인하도록 만들었다.",
            "토큰 검증 로직을 서비스 호출 전에 두면 잘못된 요청을 빠르게 차단할 수 있다.",
            "회원가입과 로그인 기능이 있어야 게시글 작성자와 실제 계정을 연결할 수 있다.",
        ],
    },
    {
        "name": "deploy",
        "display": "배포",
        "tags": ["deploy", "render", "vercel", "env"],
        "titles": [
            "Render와 Vercel로 첫 배포한 기록",
            "환경변수 설정 때문에 막혔던 부분 정리",
            "프론트와 백엔드 주소를 분리한 배포 메모",
            "배포 후 실제 서비스 확인 체크리스트",
        ],
        "sentences": [
            "배포에서는 로컬 주소를 그대로 쓰면 안 되기 때문에 환경변수로 API 주소를 분리했다.",
            "Render DB와 Web Service의 지역을 맞춰야 연결 문제가 덜 발생한다.",
            "Vercel은 프론트 배포에 편하지만 백엔드 API 주소를 별도로 관리해야 한다.",
            "배포 후에는 회원가입, 로그인, 게시글 작성, 검색, 댓글 작성 순으로 다시 확인했다.",
        ],
    },
    {
        "name": "error",
        "display": "오류 해결",
        "tags": ["debug", "error", "troubleshooting"],
        "titles": [
            "404와 500 오류를 구분해서 본 기록",
            "포트 충돌 문제를 해결한 메모",
            "컴파일 에러를 클래스 기준으로 찾는 방법",
            "API 응답이 비어 있을 때 확인한 순서",
        ],
        "sentences": [
            "404는 경로 자체가 없을 때, 500은 서버 내부 로직에서 예외가 발생할 때라는 점을 다시 확인했다.",
            "포트 8080을 다른 java 프로세스가 쓰고 있으면 서버가 바로 뜨지 않을 수 있다.",
            "컴파일 에러는 보통 import 누락이나 생성자 인자 개수 불일치부터 확인하는 것이 빠르다.",
            "브라우저 화면만 보지 말고 네트워크 탭과 서버 로그를 같이 보는 습관이 중요하다.",
        ],
    },
    {
        "name": "rag",
        "display": "RAG",
        "tags": ["rag", "embedding", "pgvector", "ai"],
        "titles": [
            "게시판 RAG 검색 실험 기록",
            "임베딩 기반 게시글 검색 구조 정리",
            "pgvector를 붙여 본 후기",
            "RAG 답변 품질을 평가한 메모",
        ],
        "sentences": [
            "질문을 임베딩으로 바꾸고 유사한 게시글 청크를 찾은 뒤 LLM이 답변을 생성하도록 구성했다.",
            "pgvector를 PostgreSQL 안에서 사용하니 기존 DB와 함께 관리할 수 있어서 단순했다.",
            "검색 품질은 청크 길이보다 실제 글 내용의 정보량에 더 큰 영향을 받았다.",
            "Ragas로 faithfulness와 answer relevancy를 확인하면서 프롬프트를 조정했다.",
        ],
    },
    {
        "name": "search",
        "display": "검색과 페이징",
        "tags": ["search", "paging", "query"],
        "titles": [
            "검색과 페이지네이션을 함께 구현한 과정",
            "검색어 상태와 실제 검색 요청을 분리한 이유",
            "페이지 이동 시 목록 상태를 유지한 기록",
            "제목과 내용 검색을 추가하면서 정리한 메모",
        ],
        "sentences": [
            "검색어 입력 중마다 서버 호출을 보내지 않고 버튼이나 Enter로 요청하도록 바꿨다.",
            "검색 결과가 한 페이지에 너무 많지 않도록 size를 고정하고 totalPages를 같이 받도록 했다.",
            "검색을 수행할 때는 페이지를 0으로 되돌리는 것이 자연스럽다.",
            "목록 조회 API에서 keyword가 비어 있으면 전체 목록을 반환하게 해서 재사용성을 높였다.",
        ],
    },
]


class ApiError(Exception):
    def __init__(self, status: int, body: str):
        self.status = status
        self.body = body
        super().__init__(f"HTTP {status}: {body}")


@dataclass
class SeedUser:
    login_id: str
    nickname: str


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Seed the deployed AI board with dummy users, posts, and comments.")
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL, help="Board backend base URL")
    parser.add_argument("--users", type=int, default=120, help="Target user count")
    parser.add_argument("--posts", type=int, default=1200, help="Target post count")
    parser.add_argument("--comments", type=int, default=1800, help="Target comment count")
    parser.add_argument("--password", default=DEFAULT_PASSWORD, help="Password used for generated users")
    parser.add_argument("--prefix", default=f"seed{datetime.now().strftime('%Y%m%d%H%M')}", help="Login ID prefix for generated users")
    parser.add_argument("--state-file", default=str(DEFAULT_STATE_FILE), help="Path to the local state file")
    parser.add_argument("--post-workers", type=int, default=2, help="Concurrent workers for post creation")
    parser.add_argument("--comment-workers", type=int, default=4, help="Concurrent workers for comment creation")
    parser.add_argument("--timeout", type=int, default=90, help="Per-request timeout in seconds")
    parser.add_argument("--retry-count", type=int, default=3, help="Retry count for transient failures")
    parser.add_argument("--sleep-ms", type=int, default=120, help="Delay between requests in milliseconds")
    return parser


def random_choice_weighted() -> str:
    value = random.random()

    if value < SHORT_POST_WEIGHT:
        return "short"

    if value < SHORT_POST_WEIGHT + MEDIUM_POST_WEIGHT:
        return "medium"

    return "long"


def normalize_url(base_url: str) -> str:
    return base_url.rstrip("/")


def load_state(state_path: Path, args: argparse.Namespace) -> dict[str, Any]:
    if state_path.exists():
        return json.loads(state_path.read_text(encoding="utf-8"))

    return {
        "baseUrl": normalize_url(args.base_url),
        "prefix": args.prefix,
        "users": [],
        "posts": [],
        "commentsCreated": 0,
        "createdAt": datetime.now().isoformat(),
    }


def save_state(state_path: Path, state: dict[str, Any]) -> None:
    state_path.parent.mkdir(parents=True, exist_ok=True)
    state_path.write_text(json.dumps(state, ensure_ascii=False, indent=2), encoding="utf-8")


def request_json(
    method: str,
    url: str,
    payload: dict[str, Any] | None,
    token: str | None,
    timeout: int,
) -> Any:
    data = None
    headers = {"Accept": "application/json"}

    if payload is not None:
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json; charset=utf-8"

    if token is not None:
        headers["Authorization"] = f"Bearer {token}"

    request = urllib.request.Request(url, data=data, headers=headers, method=method)

    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            response_body = response.read().decode("utf-8")

            if response_body == "":
                return None

            return json.loads(response_body)
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", errors="replace")
        raise ApiError(error.code, body) from error


def try_parse_message(body: str) -> str:
    try:
        parsed = json.loads(body)
    except json.JSONDecodeError:
        return body

    if isinstance(parsed, dict):
        return str(parsed.get("message") or parsed.get("error") or parsed)

    return str(parsed)


def login(base_url: str, login_id: str, password: str, timeout: int) -> str:
    response = request_json(
        "POST",
        f"{base_url}/api/users/login",
        {"loginId": login_id, "password": password},
        None,
        timeout,
    )
    return response["token"]


def signup_or_login(base_url: str, user: SeedUser, password: str, timeout: int) -> str:
    try:
        request_json(
            "POST",
            f"{base_url}/api/users/signup",
            {
                "loginId": user.login_id,
                "password": password,
                "nickname": user.nickname,
            },
            None,
            timeout,
        )
    except ApiError as error:
        if error.status not in {400, 409}:
            raise

    return login(base_url, user.login_id, password, timeout)


def build_seed_users(prefix: str, target_count: int) -> list[SeedUser]:
    users: list[SeedUser] = []
    nickname_prefix = prefix[-8:]

    for index in range(1, target_count + 1):
        login_id = f"{prefix}_user_{index:03d}"
        nickname = f"시드{nickname_prefix}{index:03d}"
        users.append(SeedUser(login_id=login_id, nickname=nickname))

    return users


def build_post_title(topic: dict[str, Any], index: int) -> str:
    title = random.choice(topic["titles"])
    return f"{title} #{index:04d}"


def build_post_content(topic: dict[str, Any], length_type: str, index: int) -> str:
    intro = f"{topic['display']} 주제로 정리한 게시글 {index}번입니다."
    selected = random.sample(topic["sentences"], k=min(4, len(topic["sentences"])))
    sentences = [intro] + selected

    if length_type == "short":
        return " ".join(sentences[:2])

    if length_type == "medium":
        extra = [
            "구현하면서 막힌 부분과 해결한 순서를 같이 적어 두었다.",
            "다음 번에 같은 기능을 만들 때 바로 참고할 수 있도록 요청 흐름도 함께 메모했다.",
            "화면과 API가 연결되는 지점을 기준으로 확인하니 디버깅이 쉬웠다.",
        ]
        return " ".join(sentences + random.sample(extra, k=2))

    long_sections = [
        "구현 순서: 먼저 최소 엔티티를 만들고, Repository를 연결하고, Service에서 검증과 저장 로직을 정리한 뒤 Controller를 연결했다.",
        "확인한 점: 요청 본문 구조가 백엔드 DTO와 맞는지, 응답 JSON에 필요한 필드가 빠지지 않았는지, 권한 검사 분기가 제대로 동작하는지 점검했다.",
        "회고: 처음에는 화면과 서버를 한 번에 보려다 복잡해졌지만, 기능 단위로 잘라서 확인하니 훨씬 이해하기 쉬웠다.",
        "다음 작업: 테스트 데이터를 조금 더 넣고 검색, 태그, 페이징, 인증 흐름을 통합해서 다시 확인할 예정이다.",
    ]
    repeated = random.sample(topic["sentences"], k=min(4, len(topic["sentences"])))
    content_parts = sentences + long_sections + repeated + repeated
    return "\n\n".join(content_parts)


def build_post_tags(topic: dict[str, Any]) -> list[str]:
    tag_count = random.randint(1, min(3, len(topic["tags"])))
    return random.sample(topic["tags"], k=tag_count)


def build_comment_content(post_title: str, comment_index: int) -> str:
    templates = [
        f"{post_title} 내용이 정리가 잘 되어 있어서 흐름 이해에 도움이 됐습니다.",
        f"{comment_index}번째 댓글입니다. 구현 순서 설명이 구체적이라 다시 따라 해보기 좋았습니다.",
        "같은 문제를 겪고 있었는데 해결 순서가 적혀 있어서 바로 확인해볼 수 있었습니다.",
        "검색과 태그를 같이 설명한 부분이 특히 유용했습니다. 다음 단계도 기대됩니다.",
        "권한 처리와 예외 처리 분기가 어떻게 연결되는지 조금 더 실습해보고 싶습니다.",
    ]
    return random.choice(templates)


def create_post_payload(index: int) -> tuple[dict[str, Any], dict[str, Any]]:
    topic = random.choice(TOPICS)
    length_type = random_choice_weighted()
    payload = {
        "title": build_post_title(topic, index),
        "content": build_post_content(topic, length_type, index),
        "tagNames": build_post_tags(topic),
    }
    return topic, payload


def with_retries(func, retry_count: int, sleep_seconds: float):
    last_error = None

    for attempt in range(1, retry_count + 1):
        try:
            return func()
        except ApiError as error:
            last_error = error

            if error.status not in {401, 429, 500, 502, 503, 504}:
                raise
        except Exception as error:  # noqa: BLE001
            last_error = error

        time.sleep(sleep_seconds * attempt)

    if last_error is not None:
        raise last_error

    raise RuntimeError("Retry wrapper ended without result or exception.")


class TokenStore:
    def __init__(self, base_url: str, password: str, timeout: int):
        self.base_url = base_url
        self.password = password
        self.timeout = timeout
        self.tokens: dict[str, str] = {}

    def set_token(self, login_id: str, token: str) -> None:
        self.tokens[login_id] = token

    def get_token(self, user: SeedUser) -> str:
        token = self.tokens.get(user.login_id)

        if token is None:
            token = login(self.base_url, user.login_id, self.password, self.timeout)
            self.tokens[user.login_id] = token

        return token

    def refresh_token(self, user: SeedUser) -> str:
        token = login(self.base_url, user.login_id, self.password, self.timeout)
        self.tokens[user.login_id] = token
        return token


def create_post(
    base_url: str,
    user: SeedUser,
    payload: dict[str, Any],
    token_store: TokenStore,
    timeout: int,
) -> dict[str, Any]:
    request_body = {
        "title": payload["title"],
        "content": payload["content"],
        "authorName": user.nickname,
        "tagNames": payload["tagNames"],
    }

    def do_request() -> dict[str, Any]:
        token = token_store.get_token(user)

        try:
            return request_json(
                "POST",
                f"{base_url}/api/posts",
                request_body,
                token,
                timeout,
            )
        except ApiError as error:
            if error.status == 401:
                token = token_store.refresh_token(user)
                return request_json(
                    "POST",
                    f"{base_url}/api/posts",
                    request_body,
                    token,
                    timeout,
                )

            raise

    return do_request()


def create_comment(
    base_url: str,
    user: SeedUser,
    post_id: int,
    post_title: str,
    comment_index: int,
    token_store: TokenStore,
    timeout: int,
) -> dict[str, Any]:
    request_body = {
        "content": build_comment_content(post_title, comment_index),
        "authorName": user.nickname,
    }

    def do_request() -> dict[str, Any]:
        token = token_store.get_token(user)

        try:
            return request_json(
                "POST",
                f"{base_url}/api/posts/{post_id}/comments",
                request_body,
                token,
                timeout,
            )
        except ApiError as error:
            if error.status == 401:
                token = token_store.refresh_token(user)
                return request_json(
                    "POST",
                    f"{base_url}/api/posts/{post_id}/comments",
                    request_body,
                    token,
                    timeout,
                )

            raise

    return do_request()


def ensure_users(
    state: dict[str, Any],
    seed_users: list[SeedUser],
    args: argparse.Namespace,
    token_store: TokenStore,
) -> list[SeedUser]:
    existing_users = {
        item["loginId"]: SeedUser(login_id=item["loginId"], nickname=item["nickname"])
        for item in state["users"]
    }

    result_users: list[SeedUser] = []

    for user in seed_users:
        if user.login_id in existing_users:
            result_users.append(existing_users[user.login_id])
            continue

        token = with_retries(
            lambda: signup_or_login(args.base_url, user, args.password, args.timeout),
            args.retry_count,
            args.sleep_ms / 1000,
        )

        token_store.set_token(user.login_id, token)
        state["users"].append({"loginId": user.login_id, "nickname": user.nickname})
        result_users.append(user)
        print(f"[USER {len(state['users'])}/{args.users}] {user.login_id}")

        if len(state["users"]) % 10 == 0:
            save_state(Path(args.state_file), state)

        time.sleep(args.sleep_ms / 1000)

    save_state(Path(args.state_file), state)
    return result_users


def seed_posts(
    state: dict[str, Any],
    users: list[SeedUser],
    args: argparse.Namespace,
    token_store: TokenStore,
) -> None:
    existing_count = len(state["posts"])

    if existing_count >= args.posts:
        print(f"게시글은 이미 {existing_count}개 이상 준비되어 있습니다.")
        return

    next_index = existing_count + 1
    sleep_seconds = args.sleep_ms / 1000

    with ThreadPoolExecutor(max_workers=args.post_workers) as executor:
        futures = {}

        while len(state["posts"]) < args.posts:
            while len(futures) < args.post_workers and next_index <= args.posts:
                user = random.choice(users)
                _, payload = create_post_payload(next_index)
                future = executor.submit(
                    with_retries,
                    lambda user=user, payload=payload: create_post(
                        args.base_url,
                        user,
                        payload,
                        token_store,
                        args.timeout,
                    ),
                    args.retry_count,
                    sleep_seconds,
                )
                futures[future] = {
                    "index": next_index,
                    "ownerLoginId": user.login_id,
                    "authorName": user.nickname,
                }
                next_index += 1
                time.sleep(sleep_seconds)

            completed = next(as_completed(futures))
            meta = futures.pop(completed)
            response = completed.result()

            state["posts"].append(
                {
                    "id": response["id"],
                    "title": response["title"],
                    "ownerLoginId": response["ownerLoginId"],
                    "authorName": response["authorName"],
                }
            )

            created_count = len(state["posts"])
            print(
                f"[POST {created_count}/{args.posts}] "
                f"id={response['id']} owner={meta['ownerLoginId']}"
            )

            if created_count % 10 == 0:
                save_state(Path(args.state_file), state)

    save_state(Path(args.state_file), state)


def seed_comments(
    state: dict[str, Any],
    users: list[SeedUser],
    args: argparse.Namespace,
    token_store: TokenStore,
) -> None:
    existing_count = int(state.get("commentsCreated", 0))

    if existing_count >= args.comments:
        print(f"댓글은 이미 {existing_count}개 이상 준비되어 있습니다.")
        return

    posts = state["posts"]

    if len(posts) == 0:
        raise RuntimeError("댓글을 만들 게시글이 없습니다.")

    next_index = existing_count + 1
    sleep_seconds = args.sleep_ms / 1000

    with ThreadPoolExecutor(max_workers=args.comment_workers) as executor:
        futures = {}

        while state["commentsCreated"] < args.comments:
            while len(futures) < args.comment_workers and next_index <= args.comments:
                user = random.choice(users)
                post = random.choice(posts)
                future = executor.submit(
                    with_retries,
                    lambda user=user, post=post, next_index=next_index: create_comment(
                        args.base_url,
                        user,
                        post["id"],
                        post["title"],
                        next_index,
                        token_store,
                        args.timeout,
                    ),
                    args.retry_count,
                    sleep_seconds,
                )
                futures[future] = {"index": next_index}
                next_index += 1
                time.sleep(sleep_seconds / 2)

            completed = next(as_completed(futures))
            futures.pop(completed)
            response = completed.result()
            state["commentsCreated"] += 1

            print(
                f"[COMMENT {state['commentsCreated']}/{args.comments}] "
                f"id={response['id']} post={response['post']['id']}"
            )

            if state["commentsCreated"] % 20 == 0:
                save_state(Path(args.state_file), state)

    save_state(Path(args.state_file), state)


def print_summary(state: dict[str, Any], state_path: Path) -> None:
    print("")
    print("시드 작업 요약")
    print(f"- 사용자: {len(state['users'])}")
    print(f"- 게시글: {len(state['posts'])}")
    print(f"- 댓글: {state['commentsCreated']}")
    print(f"- 상태 파일: {state_path}")


def main() -> int:
    parser = build_arg_parser()
    args = parser.parse_args()

    random.seed()
    args.base_url = normalize_url(args.base_url)
    state_path = Path(args.state_file)
    state = load_state(state_path, args)
    token_store = TokenStore(args.base_url, args.password, args.timeout)

    seed_users = build_seed_users(state["prefix"], args.users)
    users = ensure_users(state, seed_users, args, token_store)
    seed_posts(state, users, args, token_store)
    seed_comments(state, users, args, token_store)
    print_summary(state, state_path)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        print("\n작업이 중단되었습니다. 같은 상태 파일로 다시 실행하면 이어서 진행됩니다.")
        raise
