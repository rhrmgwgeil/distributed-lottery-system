@echo off
setlocal
@REM Pin the JDK to version 21 for this project's compilation
set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.2"
set "PATH=%JAVA_HOME%\bin;%PATH%"

@REM Execute the discovered system Maven executable directly
"d:\SI1396\apache-maven-3.9.9\bin\mvn.cmd" %*
endlocal
