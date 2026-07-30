@echo off
echo ========================================
echo   Avvio ms-base-prj (Spring Boot)
echo ========================================
cd /d "%~dp0"

cd ../..

call :load_env
echo Compilazione in corso...
call mvnw.cmd -q clean package -DskipTests
if errorlevel 1 (
    echo ERRORE: build fallita.
    exit /b 1
)
echo Avvio server...
"%JAVA_HOME%\bin\java" -jar target\ms-base-prj-0.0.1-SNAPSHOT.jar
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
