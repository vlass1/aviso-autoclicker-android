@echo off
set DIR=%~dp0
set GRADLE_USER_HOME=%DIR%\.gradle

java -jar "%DIR%\gradle\wrapper\gradle-wrapper.jar" %*
