@echo off
title TopoManager - Execucao Nativa
echo Iniciando TopoManager com 5GB de RAM usando o JDK local (\jdk-21.0.8\bin)...

:: Aponta diretamente para o java.exe na pasta relativa sem alterar as variaveis globais do sistema
set "LOCAL_JAVA=%~dp0jdk-21.0.8\bin\java.exe"

:: Executa a aplicacao utilizando exclusivamente o Java local com os parametros de memoria e GC
"%LOCAL_JAVA%" -Xms2g -Xmx5g -XX:+UseG1GC -jar topomanager.jar

pause