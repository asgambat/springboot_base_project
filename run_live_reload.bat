@echo off
echo ========================================
echo   Avvio ms-base-prj (Live Reload)
echo ========================================
cd /d "%~dp0"
call :load_env
call mvnw.cmd spring-boot:run
goto :eof

:load_env
if not exist .env (
    echo ERRORE: file .env non trovato. Crea un file .env con JDK_PATH=percorso_jdk
    exit /b 1
)
for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
    if "%%A"=="JDK_PATH" (
        set "JAVA_HOME=%%B"
        set "PATH=%%B\bin;%PATH%"
    )
)
if not defined JAVA_HOME (
    echo ERRORE: JDK_PATH non definito in .env
    exit /b 1
)
goto :eof
