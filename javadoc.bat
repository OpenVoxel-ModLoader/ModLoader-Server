@echo off
rem Generates the server plugin API Javadoc (Permissions) into dist\javadoc.
setlocal
cd /d "%~dp0"
set "SERVER=%OPENVOXEL_SERVER_JAR%"
if "%SERVER%"=="" if exist "server\openvoxel-server.jar" set "SERVER=server\openvoxel-server.jar"
if "%SERVER%"=="" if exist "..\..\..\ServerTest\openvoxel-server-0.0.8-SNAPSHOT.jar" set "SERVER=..\..\..\ServerTest\openvoxel-server-0.0.8-SNAPSHOT.jar"
if "%SERVER%"=="" ( echo Server JAR not found; set OPENVOXEL_SERVER_JAR. & exit /b 1 )
if exist dist\javadoc rmdir /s /q dist\javadoc
javadoc --release 11 -quiet -Xdoclint:none -encoding UTF-8 -charset UTF-8 -windowtitle "OpenVoxel ModLoader server API" -doctitle "OpenVoxel ModLoader server API" -sourcepath src\main\java -classpath "%SERVER%" -link https://docs.oracle.com/en/java/javase/11/docs/api/ -d dist\javadoc src\main\java\com\happyuky7\openvoxelmoding\server\permissions\Permissions.java src\main\java\com\happyuky7\openvoxelmoding\server\permissions\package-info.java
if errorlevel 1 exit /b 1
echo Javadoc in dist\javadoc
