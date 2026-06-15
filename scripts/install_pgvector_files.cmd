@echo off
setlocal

set "PGROOT=C:\Program Files\PostgreSQL\16"
set "BUILD_DIR=C:\week15-16_team03_ai_board_lab\.pgvector-build"

if not exist "%PGROOT%\lib" (
  echo PostgreSQL lib directory not found.
  exit /b 1
)

copy /Y "%BUILD_DIR%\vector.dll" "%PGROOT%\lib\vector.dll"
if errorlevel 1 exit /b 1

copy /Y "%BUILD_DIR%\vector.control" "%PGROOT%\share\extension\vector.control"
if errorlevel 1 exit /b 1

copy /Y "%BUILD_DIR%\sql\vector--*.sql" "%PGROOT%\share\extension\"
if errorlevel 1 exit /b 1

if not exist "%PGROOT%\include\server\extension\vector" mkdir "%PGROOT%\include\server\extension\vector"
if errorlevel 1 exit /b 1

copy /Y "%BUILD_DIR%\src\halfvec.h" "%PGROOT%\include\server\extension\vector\halfvec.h"
if errorlevel 1 exit /b 1

copy /Y "%BUILD_DIR%\src\sparsevec.h" "%PGROOT%\include\server\extension\vector\sparsevec.h"
if errorlevel 1 exit /b 1

copy /Y "%BUILD_DIR%\src\vector.h" "%PGROOT%\include\server\extension\vector\vector.h"
if errorlevel 1 exit /b 1

echo pgvector files installed successfully.
