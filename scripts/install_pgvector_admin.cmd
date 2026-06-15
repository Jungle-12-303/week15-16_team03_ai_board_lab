@echo off
setlocal

call "C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\Common7\Tools\VsDevCmd.bat" -arch=x64
if errorlevel 1 exit /b 1

set "PGROOT=C:\Program Files\PostgreSQL\16"
set "BUILD_DIR=C:\week15-16_team03_ai_board_lab\.pgvector-build"

cd /d "%BUILD_DIR%"

echo Installing pgvector into PostgreSQL 16...
nmake /F Makefile.win install
if errorlevel 1 exit /b 1

echo pgvector install completed.
