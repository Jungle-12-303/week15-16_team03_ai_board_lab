#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-$HOME/ai-knowledge-board}"

cd "$APP_DIR"
git pull
docker compose build
docker compose up -d
docker compose ps
docker compose logs --tail=80 backend
