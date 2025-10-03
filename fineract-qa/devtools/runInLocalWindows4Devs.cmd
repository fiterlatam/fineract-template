@echo off
setlocal enabledelayedexpansion

REM =========================================================================
REM Copyright 2015 the original author or authors.
REM
REM Licensed under the Apache License, Version 2.0 (the "License");
REM you may not use this file except in compliance with the License.
REM You may obtain a copy of the License at
REM
REM      https://www.apache.org/licenses/LICENSE-2.0
REM
REM Unless required by applicable law or agreed to in writing, software
REM distributed under the License is distributed on an "AS IS" BASIS,
REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM See the License for the specific language governing permissions and
REM limitations under the License.
REM =========================================================================

cls

REM ========================================================================
REM 1) Aguardar o servidor ficar saudável
REM ========================================================================
echo.
echo === Step 1: Waiting for Fineract to become healthy ===

set RETRIES=120
set DELAY=5
set COUNT=0

:WAIT_HEALTH
REM curl -k https://localhost:8443/fineract-provider/actuator/health >nul 2>&1
REM REM Usando localhost no lugar da variavel de ambiente do newman, para fins de health check
curl -k https://localhost:8443/fineract-provider/actuator/health >nul 2>&1
if %ERRORLEVEL%==0 (
    echo ✅ Fineract is UP!
    goto :HEALTHY
)

set /a COUNT+=1
if !COUNT! GEQ %RETRIES% (
    echo ❌ Timeout waiting for Fineract to start
    exit /b 1
)

timeout /t %DELAY% >nul
goto :WAIT_HEALTH

:HEALTHY
echo.

cls
REM ========================================================================
REM 2) Instalar Newman e Reporters
REM ========================================================================

echo.
echo === Step 2: Installing Newman and reporters ===
where newman --version > nul 2>&1
if %errorlevel% neq 0 (
    echo Newman not found. Installing now...
    call npm install -g newman newman-reporter-html newman-reporter-htmlextra
) else (
    echo Newman is already installed.
)
echo.

cls
REM ========================================================================
REM 3) Setup do ambiente e resultados
REM ========================================================================

echo.
echo === Step 3: Setting up environment and cleaning results ===
cd ../../

:: Configura caminhos
set "BASE_PATH=fineract-qa\collections\"
set "ENV_FILE=%CD%\fineract-qa\environments\EA-Localhost.postman_environment.json"
set "RESULTS_DIR=%CD%\fineract-qa\results\"
set "ATF_RESULTS_DIR=%CD%\..\atf\fineract-qa\results\"

:: Cria pastas e limpa resultados antigos
if not exist "%ATF_RESULTS_DIR%" mkdir "%ATF_RESULTS_DIR%"
del /q "%ATF_RESULTS_DIR%\*" 2>nul
del /q "%RESULTS_DIR%\*" 2>nul

:: timestamp para fins de log, caso necessário
for /f "tokens=1-4 delims=/ " %%i in ('date /t') do set DATE=%%l%%k%%j
for /f "tokens=1-2 delims=: " %%i in ('time /t') do set TIME=%%i%%j
set TIMESTAMP=%DATE%-%TIME%
set TIMESTAMP=%TIMESTAMP::=%


REM ========================================================================
REM 4) Selecionar e Executar Collections Postman
REM ========================================================================

:INPUT_LOOP_MAIN
cls
echo.
echo === Step 4: Running ATF collections ===
echo #################################################################################
echo #
echo # Select one option to execute ATF:
echo #

set "i=0"
set "TEST_FAIL=0"

:: Mapeia e lista as coleções disponíveis
for %%f in ("%BASE_PATH%*.postman_collection.json") do (
    set /a i+=1
    set "COLLECTION_PATH[!i!]=%%~dpfnf"
    echo # [!i!] - %%~nf
)

:: Verifica se encontrou coleções
if !i! equ 0 (
    echo # No collection found at this folder "%BASE_PATH%"
    echo #################################################################################
    goto :eof
)

echo #
echo # [A] - Execute All collections
echo # [Q] - Quit
echo #

:INPUT_LOOP
set /p "CHOICE=#  Type the Collection Id, 'A' for All, or 'Q' to quit: "

:: 2. Valida a entrada do usuário
if /i "%CHOICE%"=="Q" (
    echo # Saindo...
    goto :eof
) else if /i "%CHOICE%"=="A" (
    set "START_INDEX=1"
    set "END_INDEX=!i!"
    goto :EXECUTE
) else if /i "%CHOICE%"=="" (
    set "START_INDEX=1"
    set "END_INDEX=!i!"
    goto :EXECUTE
)

:: Verifica se é um número e se está no range [1, !i!]
set "VALID_CHOICE="
for /l %%N in (1, 1, !i!) do (
    if "%CHOICE%"=="%%N" (
        set "VALID_CHOICE=1"
        set "START_INDEX=%%N"
        set "END_INDEX=%%N"
        goto :EXECUTE
    )
)

:: Se chegou aqui, a escolha foi inválida
if not defined VALID_CHOICE (
    echo # Invalid option. Please try again.
    goto :INPUT_LOOP
)

:: 3. Executa a(s) colecao(oes)
:EXECUTE
set "TEST_FAIL=0"

echo #
echo #################################################################################
echo #

if "%START_INDEX%"=="1" if "%END_INDEX%"=="!i!" (
    echo # Executing All collections...
) else (
    echo # Executing selected Collection: !START_INDEX!
)

:: Usamos START_INDEX e END_INDEX no loop numérico (for /l)
for /l %%k in (!START_INDEX!, 1, !END_INDEX!) do (

    :: RECUPERA O CAMINHO DA COLEÇÃO USANDO O ÍNDICE %%k
    set "COLLECTION_PATH=!COLLECTION_PATH[%%k]!"

    :: Extrai o nome do arquivo da variável COLLECTION_PATH
    for %%c in ("!COLLECTION_PATH!") do (
        set "FILE_NAME=%%~nc"
    )

    call newman run "!COLLECTION_PATH!" ^
        -e "%ENV_FILE%" ^
        -k ^
        --verbose ^
        --reporter-cli-show-body ^
        --reporter-json-export "%RESULTS_DIR%!FILE_NAME!.json" ^
        --reporter-htmlextra-export "%CD%\fineract-qa\results\!FILE_NAME!.html" > "%CD%\..\atf\fineract-qa\results\!FILE_NAME!.txt"

    if errorlevel 1 (
        echo # - !FILE_NAME! --- X failed
        set TEST_FAIL=1
    ) else (
        echo # - !FILE_NAME! --- OK
    )
)

if "%TEST_FAIL%"=="1" (
    echo #
    echo #
    echo # - One or more collections failed. :-(
    echo # - Check results at "%CD%\..\atf\fineract-qa\results\*.txt"
    echo #
    echo #################################################################################
    pause
    GOTO :INPUT_LOOP_MAIN
)

echo #
echo #
echo # - All collections executed successfully! ;-)
echo #
echo #################################################################################

pause
GOTO :INPUT_LOOP_MAIN