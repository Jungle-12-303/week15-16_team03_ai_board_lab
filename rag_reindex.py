#!/usr/bin/env python3
import argparse
import getpass
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parent


def load_dotenv():
    dotenv = ROOT_DIR / ".env"
    if not dotenv.exists():
        return

    for line in dotenv.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue

        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip().strip("'").strip('"'))


def request_json(method, base_url, path, data=None, token=None, timeout=180):
    body = None
    headers = {"Accept": "application/json"}

    if data is not None:
        body = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"

    if token:
        headers["Authorization"] = f"Bearer {token}"

    request = urllib.request.Request(
        base_url.rstrip("/") + path,
        data=body,
        headers=headers,
        method=method,
    )

    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            raw = response.read().decode("utf-8")
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as error:
        raw = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {path} failed with HTTP {error.code}: {raw}") from error
    except urllib.error.URLError as error:
        raise RuntimeError(f"{method} {path} failed: {error}") from error


def login(base_url, email, password, timeout):
    response = request_json(
        "POST",
        base_url,
        "/api/auth/login",
        {"email": email, "password": password},
        timeout=timeout,
    )
    return response["token"]


def resolve_token(args):
    token = args.token or os.environ.get("RAG_REINDEX_TOKEN")
    if token:
        return token

    email = args.email or os.environ.get("RAG_REINDEX_EMAIL")
    password = args.password or os.environ.get("RAG_REINDEX_PASSWORD")

    if not email:
        email = input("Email: ").strip()

    if not password:
        password = getpass.getpass("Password: ")

    return login(args.base_url, email, password, args.timeout)


def confirm(args):
    if args.yes:
        return

    print("This will delete existing post vectors and rebuild vector_store from all posts.")
    answer = input("Continue? [y/N]: ").strip().lower()
    if answer not in {"y", "yes"}:
        raise RuntimeError("Canceled.")


def main():
    load_dotenv()

    parser = argparse.ArgumentParser(description="Reindex all post vectors through the running Spring Boot API.")
    parser.add_argument("--base-url", default=os.environ.get("RAG_REINDEX_BASE_URL", "http://localhost:8080"))
    parser.add_argument("--email", default=None)
    parser.add_argument("--password", default=None)
    parser.add_argument("--token", default=None)
    parser.add_argument("--timeout", type=int, default=180)
    parser.add_argument("--yes", action="store_true", help="Skip confirmation prompt.")
    args = parser.parse_args()

    confirm(args)

    token = resolve_token(args)
    result = request_json("POST", args.base_url, "/api/ai/rag/reindex", {}, token=token, timeout=args.timeout)

    indexed_posts = result.get("indexedPosts")
    if indexed_posts is None:
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return

    print(f"RAG reindex completed. indexedPosts={indexed_posts}")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nCanceled.", file=sys.stderr)
        sys.exit(130)
    except Exception as error:
        print(f"ERROR: {error}", file=sys.stderr)
        sys.exit(1)
