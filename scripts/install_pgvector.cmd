@echo off
setlocal

call "C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\Common7\Tools\VsDevCmd.bat" -arch=x64
if errorlevel 1 exit /b 1

set "PGROOT=C:\Program Files\PostgreSQL\16"
set "BUILD_DIR=C:\week15-16_team03_ai_board_lab\.pgvector-build"

if not exist "%PGROOT%\include\server\postgres.h" (
  echo PostgreSQL server headers not found.
  exit /b 1
)

if exist "%BUILD_DIR%\.git" (
  echo Updating existing pgvector source...
  git -C "%BUILD_DIR%" pull
) else (
  if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
  echo Cloning pgvector source...
  git clone https://github.com/pgvector/pgvector.git "%BUILD_DIR%"
)

if errorlevel 1 exit /b 1

cd /d "%BUILD_DIR%"

echo Building pgvector...
nmake /F Makefile.win
if errorlevel 1 exit /b 1

echo Installing pgvector into PostgreSQL 16...
nmake /F Makefile.win install
if errorlevel 1 exit /b 1

echo pgvector build and install completed.
