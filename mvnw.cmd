@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
if defined PYTHON (
  "%PYTHON%" "%SCRIPT_DIR%tools\build\maven_wrapper.py" %*
  exit /b %ERRORLEVEL%
)
where python >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  python "%SCRIPT_DIR%tools\build\maven_wrapper.py" %*
  exit /b %ERRORLEVEL%
)
where py >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  py -3.12 "%SCRIPT_DIR%tools\build\maven_wrapper.py" %*
  exit /b %ERRORLEVEL%
)
echo Python 3.12.13 is required. Set PYTHON to its executable path. 1>&2
exit /b 2
