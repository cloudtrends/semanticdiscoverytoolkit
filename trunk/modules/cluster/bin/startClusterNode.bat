
@ECHO off

REM Script to start a single cluster node (jvm).
REM
REM arg1: a unique id number to distinguish multiple jvm's logs from each other
REM

SET UIDNUM=%1
SET LOGDIR=%HOMEPATH%\cluster\log
SET TIMESTAMP=%DATE%-%TIME%
SET TS0=%TIMESTAMP: =-%
SET TS1=%TS0::=-%
SET TS2=%TS1:.=-%
SET TS=%TS2:/=-%

IF NOT EXIST %LOGDIR% mkdir %LOGDIR%

for /F %%g in (%HOMEPATH%/cluster/conf/active-heap-size.txt) do (
  SET HEAP_SIZE=%%g
)

set JVM_MEM=%HEAP_SIZE%
set LOG=%LOGDIR%\log-%TS%-%UIDNUM%

REM echo %LOG%

START /b run.bat org.sd.cluster.config.ClusterNode %UIDNUM% 1> "%LOG%.out" 2> "%LOG%.err"
