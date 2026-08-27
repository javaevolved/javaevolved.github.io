@echo off
setlocal DisableDelayedExpansion

where java >nul 2>&1
if errorlevel 1 (
  >&2 echo {"error":"java not found on PATH; install Java 8 or newer and configure PATH"}
  exit /b 127
)

set "SCRIPT_DIR=%~dp0"
java -jar "%SCRIPT_DIR%detect-java-version.jar" %*
exit /b %ERRORLEVEL%
