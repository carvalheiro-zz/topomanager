@echo off
title TopoManager - Execucao Nativa
echo Iniciando TopoManager com 5GB de RAM...

:: JAVA_OPTS equivalentes aos do seu docker-compose
java -Xms2g -Xmx5g -XX:+UseG1GC -jar topomanager.jar

pause