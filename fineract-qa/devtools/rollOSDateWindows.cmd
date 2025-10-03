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
setlocal

:: ====================================================================
:: CONFIGURAÇÕES
:: ====================================================================
set DATA_INICIAL=15/09/2025
set DATA_FINAL=01/10/2025

set DIA_ATUAL=%DATA_INICIAL%
set SERVICO_TEMPO=W32Time

echo.
echo ========================================================
echo INICIO DO TESTE DE DATA
echo ========================================================
echo.

:: ========================================================
:: 1. DESLIGAR SINCRONISMO DE DATA
:: ========================================================
echo [1/2] Desligando o servico de Sincronizacao de Tempo (%SERVICO_TEMPO%)...

:: 1.1 Para o Servico
net stop %SERVICO_TEMPO%

:: 1.2 Desativa o Servico
sc config %SERVICO_TEMPO% start= disabled

echo Sincronismo desligado. Iniciando loop de datas.
echo.

:LOOP
    echo.
    echo Tentando setar a data para: %DIA_ATUAL%

    :: Tenta setar a data do sistema (REQUER ADMINISTRADOR!)
    date %DIA_ATUAL%

    if errorlevel 1 (
        echo ERRO: Falha ao mudar a data. Execute o script como ADMINISTRADOR.
        goto FINALIZAR_ERRO
    ) else (
        echo DATA DO SISTEMA ATUALIZADA para %DIA_ATUAL%.
    )

    echo.
    echo --- PAUSA PARA TESTE (%DIA_ATUAL%) ---
    echo Pressione qualquer tecla para AVANÇAR para o proximo dia...
rem	pause
    pause > nul

    :: Checa se chegou na data final
rem    if "%DIA_ATUAL%"=="%DATA_FINAL%" (
rem       echo.
rem       echo *** ATENCAO: A data final ("%DATA_FINAL%") foi atingida! ***
rem       goto FINALIZAR_SUCESSO
rem    )
rem pause
    :: Logica para avancar 1 dia (usa PowerShell)
    for /f "tokens=*" %%a in ('powershell -Command "$data = [DateTime]::ParseExact('%DIA_ATUAL%', 'dd/MM/yyyy', $null); $data = $data.AddDays(1); $data.ToString('dd/MM/yyyy')"') do (
        set DIA_ATUAL=%%a
    )

    goto LOOP

:: ========================================================
:: 2. RELIGAR SINCRONISMO DE DATA
:: ========================================================

:FINALIZAR_SUCESSO
    echo.
    echo ========================================================
    echo TESTE CONCLUIDO
    echo ========================================================
    goto RELIGAR_SERVICO

:FINALIZAR_ERRO
    echo.
    echo ========================================================
    echo TESTE INTERROMPIDO (ERRO)
    echo ========================================================

:RELIGAR_SERVICO
    echo [2/2] Religando e ativando o servico de Sincronizacao de Tempo (%SERVICO_TEMPO%)...

    :: 2.1 Reconfigura o Servico para Automatico
    sc config %SERVICO_TEMPO% start= auto

    :: 2.2 Inicia o Servico
    net start %SERVICO_TEMPO%

    echo Sincronismo reativado. O Windows deve ajustar a data em breve.
    echo.
    echo Script finalizado.
    pause > nul
    goto :EOF