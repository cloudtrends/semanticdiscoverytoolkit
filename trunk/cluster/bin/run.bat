@ECHO off
REM Script to run a deployed class.

REM expect classes in ..\build\classes
REM expect libs in ..\lib

set JAVA_HOME=c:\Program Files\java\jdk1.6.0_07
set JDK_HOME=%JAVA_HOME%

REM Set default JVM_MEM to 500m if needed.
if NOT defined %JVM_MEM% set JVM_MEM=500

REM JAVA_OPTS="-Xmx400m -Dcom.sun.management.jmxremote"
REM JAVA_OPTS="-Xmx500m -Dcom.sun.management.jmxremote"
REM JAVA_OPTS="-Xmx800m -Dcom.sun.management.jmxremote"
REM JAVA_OPTS="-Xmx1000m -Dcom.sun.management.jmxremote"
REM JAVA_OPTS="-Xmx1500m -Dcom.sun.management.jmxremote"
set JAVA_OPTS=-Xmx%JVM_MEM%m -Dmachine.name=%MACHINE_NAME% -Dcom.sun.management.jmxremote

set JAVA_OPTS=%JAVA_OPTS% -DSDN_ROOT=..

setlocal EnableDelayedExpansion
set CP=..\build\classes
REM for %lib in `dir ..\lib\*.jar` do set CLASSPATH=%CLASSPATH% : %lib

FOR /f "tokens=*" %%g IN ('dir /b ..\lib\*.jar') DO (
  set CP=!CP!;..\lib\%%g)

echo "java %JAVA_OPTS% -cp !CP! %1 %2 %3 %4 %5 %6 %7 %8 %9"
java %JAVA_OPTS% -cp !CP! %1 %2 %3 %4 %5 %6 %7 %8 %9
