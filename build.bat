@echo off
rem Plain-javac build of the server core plugin (no Gradle needed). Equivalent to: gradlew.bat jar
setlocal
cd /d "%~dp0"
call :findjar
if "%SERVER%"=="" ( echo Server JAR not found. Set OPENVOXEL_SERVER_JAR or copy it to server\openvoxel-server.jar & exit /b 1 )
set "OUT=dist\ModLoader-v1.0.0-vgame-0.0.8-server.jar"
if exist build\classes rmdir /s /q build\classes
mkdir build\classes
if not exist dist mkdir dist
dir /s /b src\main\java\*.java > build\sources.txt
javac --release 11 -encoding UTF-8 -Xlint:-options -classpath "%SERVER%" -d build\classes @build\sources.txt
if errorlevel 1 exit /b 1
del build\sources.txt
xcopy /e /i /y /q src\main\resources build\classes >nul
jar --create --file "%OUT%" -C build\classes .
if errorlevel 1 exit /b 1
echo Built %OUT%
exit /b 0

:findjar
set "SERVER=%OPENVOXEL_SERVER_JAR%"
if "%SERVER%"=="" if exist "server\openvoxel-server.jar" set "SERVER=server\openvoxel-server.jar"
if "%SERVER%"=="" if exist "..\..\..\ServerTest\openvoxel-server-0.0.8-SNAPSHOT.jar" set "SERVER=..\..\..\ServerTest\openvoxel-server-0.0.8-SNAPSHOT.jar"
exit /b 0
