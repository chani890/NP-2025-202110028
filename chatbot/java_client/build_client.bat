@echo off
setlocal enabledelayedexpansion

set "BASE_DIR=%~dp0"
set "SRC_DIR=%BASE_DIR%src\ch02_pit_01"
set "OUT_DIR=%BASE_DIR%build\classes"

if not exist "%OUT_DIR%" (
    mkdir "%OUT_DIR%"
)

echo [compile] javac -encoding UTF-8 -d "%OUT_DIR%"
javac -encoding UTF-8 -d "%OUT_DIR%" "%SRC_DIR%\*.java"
if errorlevel 1 (
    echo [compile] 실패했습니다.
    exit /b 1
)

echo [compile] .class 파일이 "%OUT_DIR%" 에 생성되었습니다.
exit /b 0

