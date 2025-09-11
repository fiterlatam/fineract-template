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

cd ../../

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

rem start "FineractServer" cmd /c ".\gradlew --no-daemon --console=plain :fineract-provider:bootRun -q -x rat -x compileTestJava -x test --info"
.\gradlew --no-daemon --console=plain -Dspring.liquibase.enabled=false :fineract-provider:bootRun -q -x rat -x compileTestJava -x test --info > start.log

pause