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
REM ========================================================================
REM 1) Preparar Gradle Wrapper e Build
REM ========================================================================

echo.
echo === Step 1: Preparing Gradle Wrapper ===

REM Remove any java process that is holding the db connection
wmic process where "name like '%java%'" delete

cd ../../

call gradle wrapper
call .\gradlew wrapper --gradle-version=8.5
call .\gradlew wrapper --version
call .\gradlew build ^
    -x spotlessGroovyGradle ^
    -x checkstyleMain ^
    -x spotbugsMain ^
    -x compileTestJava ^
    -x test ^
    -x cucumber ^
    -x checkstyleTest ^
    -x asciidoctorPdf ^
    -x asciidoctor ^
    -x buildAsciidoc ^
    -x spotbugsTest

REM ========================================================================
REM 2) Preparar Banco (drop e create)
REM ========================================================================

echo.
echo === Step 2: Preparing Database ===
call .\gradlew dropPGDB -PdbName=fineract_default -PdbUserName=postgres -PdbUserPassword=postgres -PjdbcURL=jdbc:postgresql://localhost:5432/fineract_tenants
call .\gradlew createPGDB -PdbName=fineract_default -PdbUserName=postgres -PdbUserPassword=postgres -PjdbcURL=jdbc:postgresql://localhost:5432/fineract_tenants

REM ========================================================================
REM 3) Restaurar dump do banco
REM ========================================================================

echo.
echo === Step 3: Restoring database backup ===
set PGPASSWORD=postgres
D:\infra\postgresql-17.5-3-windows-x64-binaries\pgsql\bin\pg_restore ^
  -h localhost ^
  -p 5432 ^
  -U postgres ^
  -d fineract_default ^
  --verbose ^
  --no-owner ^
  --no-comments ^
  --no-privileges ^
  --no-publications ^
  --if-exists ^
  --clean ^
  fineract-qa\dbdump\db_backup.dump


REM ========================================================================
REM 3) Subir o servidor
REM ========================================================================
echo.
echo === Step 3: Starting Fineract server ===

REM Variáveis de ambiente Fineract
set FINERACT_DEFAULT_TENANTDB_HOSTNAME=localhost
set FINERACT_DEFAULT_TENANTDB_NAME=fineract_default-dev
set FINERACT_DEFAULT_TENANTDB_PORT=5432
set FINERACT_DEFAULT_TENANTDB_PWD=postgres
set FINERACT_DEFAULT_TENANTDB_UID=postgres
set FINERACT_HIKARI_DRIVER_SOURCE_CLASS_NAME=org.postgresql.Driver
set FINERACT_HIKARI_JDBC_URL=jdbc:postgresql://127.0.0.1:5432/entreamigos_tenants
set FINERACT_HIKARI_PASSWORD=postgres
set FINERACT_HIKARI_USERNAME=postgres

start "FineractServer" cmd /c ".\gradlew -Dspring.liquibase.enabled=false --no-daemon --console=plain :fineract-provider:bootRun -q -x rat -x compileTestJava -x test --info > start.log"
rem .\gradlew --no-daemon --console=plain :fineract-provider:bootRun -q -x rat -x compileTestJava -x test --info > start.log

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

REM ========================================================================
REM 5) Instalar Newman e Reporters
REM ========================================================================

echo.
echo === Step 4: Installing Newman and reporters ===
call npm install -g newman newman-reporter-html
call npm install -g newman-reporter-htmlextra

REM ========================================================================
REM 6) Executar Collections Postman
REM ========================================================================

echo.
echo === Step 5: Running Postman collections ===

for /f "tokens=1-4 delims=/ " %%i in ('date /t') do set DATE=%%l%%k%%j
for /f "tokens=1-2 delims=: " %%i in ('time /t') do set TIME=%%i%%j
set TIMESTAMP=%DATE%-%TIME%
set TIMESTAMP=%TIMESTAMP::=%

if not exist ..\atf\fineract-qa\results mkdir ..\atf\fineract-qa\results
del /q ..\atf\fineract-qa\results\*

set TEST_FAIL=false

for %%f in (fineract-qa\collections\*.postman_collection.json) do (
    REM Caminho absoluto da collection
    set "COLLECTION_FILE=%%~dpfnf"
    set "COLLECTION_NAME=%%~nf"

    echo 🔹 Running %%~nf ...

    call newman run "%%~dpfnf" ^
        -e "%CD%\fineract-qa\environments\EA-Localhost.postman_environment.json" ^
        -k ^
        --verbose ^
        --reporter-cli-show-body ^
        --reporter-json-export "%CD%\fineract-qa\results\%%~nf.json" ^
        --reporter-htmlextra-export "%CD%\fineract-qa\results\%%~nf.html" > "%CD%\..\atf\fineract-qa\results\%%~nf.txt"

    if errorlevel 1 (
        echo ❌ failed: %%~nf
        set TEST_FAIL=true
    ) else (
        echo ✅ OK: %%~nf
    )
)


pause

if "%TEST_FAIL%"=="true" (
    echo ⚠️ One or more collections failed.
    exit /b 1
)

echo.
echo ✨ All collections executed successfully!
