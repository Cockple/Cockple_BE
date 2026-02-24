@echo off
:: 사용법: scripts\tunnel.bat [GCP_IP]
:: 예시:   scripts\tunnel.bat 34.64.xxx.xxx

set GCP_IP=%1

if "%GCP_IP%"=="" (
  set /p GCP_IP=GCP IP 입력:
)

echo 터널링 시작: %GCP_IP%
echo   MySQL  -^> localhost:3306 -^> cockple-mysql:3306
echo   Redis  -^> localhost:6379 -^> cockple-redis:6379
echo 종료: Ctrl+C

ssh -N ^
  -L 3306:localhost:3306 ^
  -L 6379:localhost:6379 ^
  -i %USERPROFILE%\.ssh\cockple ^
  ubuntu@%GCP_IP%
