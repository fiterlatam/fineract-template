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

@echo off
cls

REM ========================================================================
REM 4) Aguardar o servidor ficar saudável
REM ========================================================================
echo.
echo === Step 4: Waiting for Fineract to become healthy ===

set RETRIES=120
set DELAY=5
set COUNT=0

:WAIT_HEALTH
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
REM 5) Instalar Newman e Reporters
REM ========================================================================

echo.
echo === Step 4: Installing Newman and reporters ===
where newman --version > nul 2>&1
if %errorlevel% neq 0 (
    echo Newman not found. Installing now...
    call npm install -g newman newman-reporter-html newman-reporter-htmlextra
) else (
    echo Newman is already installed.
)

REM ========================================================================
REM 6) Executar Collections Postman
REM ========================================================================

cls
cd ../../

echo.
echo === Step 5: Running ATF collections ===

for /f "tokens=1-4 delims=/ " %%i in ('date /t') do set DATE=%%l%%k%%j
for /f "tokens=1-2 delims=: " %%i in ('time /t') do set TIME=%%i%%j
set TIMESTAMP=%DATE%-%TIME%
set TIMESTAMP=%TIMESTAMP::=%

if not exist ..\atf\fineract-qa\results mkdir ..\atf\fineract-qa\results
del /q ..\atf\fineract-qa\results\*

set TEST_FAIL=false

echo #################################################################################
echo #

for %%f in (fineract-qa\collections\*.postman_collection.json) do (
    REM Caminho absoluto da collection
    set "COLLECTION_FILE=%%~dpfnf"
    set "COLLECTION_NAME=%%~nf"

    call newman run "%%~dpfnf" ^
        -e "%CD%\fineract-qa\environments\EA-Localhost.postman_environment.json" ^
        -k ^
        --verbose ^
        --reporter-cli-show-body ^
        --reporter-json-export "%CD%\fineract-qa\results\%%~nf.json" ^
        --reporter-htmlextra-export "%CD%\fineract-qa\results\%%~nf.html" > "%CD%\..\atf\fineract-qa\results\%%~nf.txt"

    if errorlevel 1 (
        echo # - %%~nf ---  X failed
        set TEST_FAIL=1
    ) else (
        echo # - %%~nf --- OK
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
    exit /b 1
)

echo.
echo # - All collections executed successfully! ;-)
echo #
echo #################################################################################
pause
